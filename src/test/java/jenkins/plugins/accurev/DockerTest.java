package jenkins.plugins.accurev;


import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.plugins.accurev.util.AccurevTestExtensions;
import hudson.util.Secret;
import jenkins.plugins.accurevclient.Accurev;
import jenkins.plugins.accurevclient.AccurevClient;
import org.apache.commons.lang.StringUtils;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class DockerTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();


    @Rule
    public DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/docker/docker-compose.yml")
            .build();

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
                return arg;
            }
        };
        docker.exec(options, "accurev", arguments);
        FreeStyleProject freeStyleProject = rule.createFreeStyleProject();
        Accurev accurev = Accurev.with(TaskListener.NULL, new EnvVars(),  new Launcher.LocalLauncher(TaskListener.NULL))
                .at(freeStyleProject.getBuildDir()).on(url);
        client = accurev.getClient();
        client.login().username(username).password(Secret.fromString(password)).execute();
        assertTrue(client.getInfo().getLoggedIn());
    }

    @Test
    public void dockerTest() throws Exception {
        assertTrue(url.contains(client.getInfo().getServerName()));

    }
}
