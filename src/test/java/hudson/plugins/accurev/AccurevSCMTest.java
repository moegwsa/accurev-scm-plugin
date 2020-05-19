package hudson.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.accurev.util.AccurevTestExtensions;
import hudson.util.Secret;
import hudson.util.StreamTaskListener;
import jenkins.plugins.accurevclient.Accurev;
import jenkins.plugins.accurevclient.AccurevClient;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class AccurevSCMTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Rule
    public DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/docker/docker-compose.yml")
            .build();

    protected TaskListener listener = StreamTaskListener.fromStderr();

    private AccurevClient client;

    private static String url;
    private static String host = "localhost";
    private static String port = "5050";
    private static String username;
    private static String password;

    @BeforeClass
    public static void init() throws IOException, InterruptedException {
        url = System.getenv("_ACCUREV_URL") == "" ? System.getenv("_ACCUREV_URL") : host + ":" + port;
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
    public void setUp() throws Exception {
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
                //arg.add("/bin/bash");
                arg.add("perl");
                arg.add("./updateJenkinsHook.pl");
                arg.add(jenkinsPort);
                arg.add("http://host.docker.internal");
                return arg;
            }
        };
        docker.exec(options, "accurev", arguments);
        FreeStyleProject freeStyleProject = rule.createFreeStyleProject();
        Accurev accurev = Accurev.with(TaskListener.NULL, new EnvVars(), new Launcher.LocalLauncher(TaskListener.NULL))
                .at(freeStyleProject.getBuildDir()).on(url);
        client = accurev.getClient();
        client.login().username(username).password(Secret.fromString(password)).execute();
        assertTrue(client.getInfo().getLoggedIn());
    }


    /**
     * Basic test - Create a AccurevSCM based project, check it out and build for the first time.
     * Next test that polling works correctly, make another keep / promote, check that polling finds it,
     * then build it and test the content of the workspace
     * @throws Exception
     */
    @Test
    public void testBasic() throws Exception {
        FreeStyleProject project = setupProject();

        File file = AccurevTestExtensions.createFile(project.getBuildDir().getPath(), "commitFile1", "test");
        List<String> files = new ArrayList<>();
        files.add(file.getAbsolutePath());
        client.add().add(files).comment("test").execute();
        client.promote().files(files).comment("test").execute();
        System.err.println("Commited first file");

        final FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.err.println("Created freestyle build");
        rule.assertBuildStatus(Result.SUCCESS, build);
        Thread.sleep(1000);
        assertFalse("scm polling should not detect any more changes after build", project.poll(listener).hasChanges());
        System.err.println("Build status asserted, true");

        File file2 = AccurevTestExtensions.createFile(project.getBuildDir().getPath(), "commitFile2", "test");
        files.clear();
        files.add(file2.getAbsolutePath());
        client.add().add(files).comment("test").execute();
        client.promote().files(files).comment("test").execute();


        System.err.println("Commit second file");
        assertTrue("scm polling did not detect commit2 change", project.poll(listener).hasChanges());
        System.err.println("no pulling detected");
        final FreeStyleBuild build2 = project.scheduleBuild2(0).get();
        System.err.println("Create second build");
        final Set<User> culprits = build2.getCulprits();
        System.err.println("Get culprits");
        assertEquals("The build should have only one culprit", 1, culprits.size());
        assertEquals("", username, culprits.iterator().next().getFullName());
        assertTrue(build2.getWorkspace().child(file2.getName()).exists());
        System.err.println("Assert culprits, username and file exists true");
        rule.assertBuildStatusSuccess(build2);
        assertFalse("scm polling should not detect any more changes after build", project.poll(listener).hasChanges());
    }

    private FreeStyleProject setupProject() throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        client = AccurevTestExtensions.createClientAtDir(project.getBuildDir(), url, username, password);
        StandardUsernamePasswordCredentials dummyIdCredentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "accurev", "Accurev credentials", username, password);
        String depot = AccurevTestExtensions.generateString(10);
        client.depot().create(depot).execute();
        String workspace = AccurevTestExtensions.generateString(10);
        client.workspace().create(workspace, depot).execute();
        SystemCredentialsProvider.getInstance().getCredentials().add(dummyIdCredentials);

        AccurevSCM scm = new AccurevSCM(
                AccurevSCM.createDepotList(host, port,  "accurev"),
                Collections.singletonList(new StreamSpec(depot, depot)),
                null, null
                        );
        project.setScm(scm);
        return project;
    }

}
