package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.palantir.docker.compose.DockerComposeRule;
import hudson.model.Label;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.accurev.util.AccurevTestExtensions;
import hudson.scm.ChangeLogSet;
import hudson.triggers.SCMTrigger;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class AccurevSCMStepTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();
    @Rule
    public AccurevSampleWorkspaceRule sampleWorkspace = new AccurevSampleWorkspaceRule();


    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/docker/docker-compose.yml")
            .build();


    String host = "localhost";
    String port = "5050";
    String user = "accurev_user";
    String pwd = "docker";

    @BeforeClass
    public static void testAccurevInstall() throws IOException, InterruptedException {
        assumeTrue("Can only run test with proper test setup",
                AccurevTestExtensions.checkCommandExist("accurev")
        );
    }

    @Before
    public void setupAccurevServer() throws Exception {
        sampleWorkspace.init(host, port, user, pwd);
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
        WorkflowJob p = rule.jenkins.createProject(WorkflowJob.class, "demo");
        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);
        String depot = sampleWorkspace.mkDepot();
        String workspace = sampleWorkspace.mkWorkspace(depot);
        sampleWorkspace.commit("file", user, "file");
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

        sampleWorkspace.commit("nextFile", user, "next");
        b = rule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        rule.assertLogContains("Fetching changes from Accurev stream", b);
        assertTrue(b.getArtifactManager().root().child("nextfile").isFile());
    }

    @Test @Ignore
    public void changelogAndPolling() throws Exception {
        WorkflowJob p = rule.jenkins.createProject(WorkflowJob.class, "demo");
        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);
        p.addTrigger(new SCMTrigger(""));
        rule.createOnlineSlave(Label.get("remote"));
        String depot = sampleWorkspace.mkDepot();
        String workspace = sampleWorkspace.mkWorkspace(depot);
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

        sampleWorkspace.commit("nextFile", user, "next");

        sampleWorkspace.notifyCommit(rule, depot);
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

}
