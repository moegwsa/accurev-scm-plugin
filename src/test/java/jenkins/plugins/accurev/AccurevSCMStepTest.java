package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.accurev.util.AccurevTestExtensions;
import hudson.scm.ChangeLogSet;
import hudson.triggers.SCMTrigger;
import hudson.util.Secret;
import jenkins.plugins.accurevclient.Accurev;
import jenkins.plugins.accurevclient.AccurevClient;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class AccurevSCMStepTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/docker/docker-compose.yml")
            .build();


    String host = "localhost";
    String port = "5050";

    private AccurevClient client;

    private static String url;
    private static String username;
    private static String password;

    @BeforeClass
    public static void init() throws IOException, InterruptedException {
        url = System.getenv("_ACCUREV_URL") == "" ? System.getenv("_ACCUREV_URL") : "localhost:5050";
        username = System.getenv("_ACCUREV_USERNAME") != null ? System.getenv("_ACCUREV_URL") : "accurev_user";
        password = System.getenv("_ACCUREV_PASSWORD") != null ? System.getenv("_ACCUREV_URL") : "docker";
        assumeTrue("Can only run test with proper test setup",
                AccurevTestExtensions.checkCommandExist("accurev") &&
                        StringUtils.isNotBlank(url) &&
                        StringUtils.isNotBlank(username) &&
                        StringUtils.isNotEmpty(password)
        );
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        // Get the port from the JenkinsRule - When JenkinsRule runs it starts Jenkins at a random port
        String jenkinsPort = Integer.toString(rule.getURL().getPort());
        // For docker.exec command, no options needed.
        DockerComposeExecOption options = new DockerComposeExecOption() {
            @Override
            public List<String> options() {
                return Collections.emptyList();
            }
        };
        // Exec into the container, updating the url pointing to Jenkins with the correct port
        DockerComposeExecArgument arguments = new DockerComposeExecArgument() {
            @Override
            public List<String> arguments() {
                List<String> arg = new ArrayList<>();
                arg.add("perl");
                arg.add("./updateJenkinsHook.pl");
                arg.add(jenkinsPort);
                arg.add("host.docker.internal");
                return arg;
            }
        };
        docker.exec(options, "accurev", arguments);

    }

    @Test
    public void roundtrip() throws Exception {
        AccurevStep step = new AccurevStep(host, port, "depot", "stream", "");

        Step roundtrip = new StepConfigTester(rule).configRoundTrip(step);
        rule.assertEqualDataBoundBeans(step, roundtrip);
    }

    @Test
    public void roundtrip_withcredentials() throws Exception {
        StandardUsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null, "user", "pass");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);
        AccurevStep step = new AccurevStep("host", "5050", "depot", "stream", "");
        step.setCredentialsId(c.getId());
        Step roundtrip = new StepConfigTester(rule).configRoundTrip(step);
        rule.assertEqualDataBoundBeans(step, roundtrip);
    }

    @Test
    public void basicCloneAndUpdate() throws Exception {
        System.err.println(System.getProperty("user.dir"));
        WorkflowJob p = rule.jenkins.createProject(WorkflowJob.class, "demo");
        client = AccurevTestExtensions.createClientAtDir(p.getBuildDir(), url, username, password);
        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);
        String depot = AccurevTestExtensions.generateString(10);
        client.depot().create(depot).execute();
        String workspace = AccurevTestExtensions.generateString(10);
        client.workspace().create(workspace, depot).execute();
        File file = AccurevTestExtensions.createFile(p.getBuildDir().getPath(), "file", "test");
        List<String> files = new ArrayList<>();
        files.add(file.getAbsolutePath());
        client.add().add(files).comment("test").execute();
        client.promote().files(files).comment("test").execute();

        rule.createOnlineSlave(Label.get("remote"));
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "agent any\n" +
                        "   stages {\n" +
                        "       stage('single') {\n" +
                        "           steps ('checkout') {\n" +
                        "               accurev host: '" + host + "', port: '"+  port + "', depot: '" + depot + "', stream: '" + depot + "', credentialsId: '" + c.getId() + "'  \n" +
                        "               archive '**'\n" +
                        "           }\n" +
                        "       }\n" +
                        "   }\n" +
                        "}\n", true));
        QueueTaskFuture<WorkflowRun> workflowRunQueueTaskFuture = p.scheduleBuild2(0);
        WorkflowRun b = rule.assertBuildStatusSuccess(workflowRunQueueTaskFuture);
        rule.assertLogContains("Cloning the remote Accurev stream", b);
        assertTrue(b.getArtifactManager().root().child("file").isFile());

        File file2 = AccurevTestExtensions.createFile(p.getBuildDir().getPath(), "file2", "test");
        files.clear();
        files.add(file2.getAbsolutePath());
        client.add().add(files).comment("test").execute();
        client.promote().files(files).comment("test").execute();

        b = rule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        rule.assertLogContains("Fetching changes from Accurev stream", b);
        assertTrue(b.getArtifactManager().root().child("file2").isFile());
    }

    @Test
    public void changelogAndPolling() throws Exception {
        WorkflowJob p = rule.jenkins.createProject(WorkflowJob.class, "demo");
        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);
        p.addTrigger(new SCMTrigger(""));
        rule.createOnlineSlave(Label.get("remote"));
        client = AccurevTestExtensions.createClientAtDir(p.getBuildDir(), url, username, password);
        String depot = AccurevTestExtensions.generateString(10);
        client.depot().create(depot).execute();
        String workspace = AccurevTestExtensions.generateString(10);
        client.workspace().create(workspace, depot).execute();
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "agent none \n" +
                        "stages { " +
                        "   stage('Build') { " +
                        "       agent { label 'remote' }\n" +
                        "           steps {\n" +
                        "               accurev host: '" + host + "', port: '"+  port + "', depot: '" + depot + "', stream: '" + depot + "', credentialsId: '" + c.getId() + "'  \n" +
                        "           }\n" +
                        "       }\n" +
                        "   }\n" +
                        "}\n", true));
        WorkflowRun b = rule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        rule.assertLogContains("Cloning the remote Accurev stream", b);

        File file = AccurevTestExtensions.createFile(p.getBuildDir().getPath(), "file", "test");
        List<String> files = new ArrayList<>();
        files.add(file.getAbsolutePath());
        client.add().add(files).comment("test").execute();
        client.promote().files(files).comment("test").execute();

        notifyCommit(rule, depot);
        b = p.getLastBuild();
        assertEquals(2, b.number);
        rule.assertLogContains("Fetching changes from Accurev stream", b);

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = b.getChangeSets();
        assertEquals(1, changeSets.size());
        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = changeSets.get(0);
        assertEquals(b, changeSet.getRun());
        assertEquals("accurev", changeSet.getKind());
        Iterator<? extends ChangeLogSet.Entry> iterator = changeSet.iterator();
        assertTrue(iterator.hasNext());
    }

    public void notifyCommit(JenkinsRule rule, String depot) throws Exception{
        ((SCMTrigger.DescriptorImpl)rule.jenkins.getDescriptorByType(SCMTrigger.DescriptorImpl.class)).synchronousPolling = true;
        WebResponse webResponse = rule.createWebClient().goTo("accurev/notifyCommit?host=" + host + "&port=" + port + "&depot=" + depot + "&streams=" + depot + "&transaction=1&reason=updated", "").getWebResponse();
        assertEquals(webResponse.getStatusCode(),200);
        // Since it takes some time to parse the request and add it to the queue, we wait for 1 sec.
        Thread.sleep(10000);
        rule.waitUntilNoActivity();
    }

}
