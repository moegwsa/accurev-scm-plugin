package jenkins.plugins.accurev;


import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Ignore("Only used for testing Docker instance")
public class DockerTest {

    @Rule
    public AccurevSampleWorkspaceRule sampleWorkspace = new AccurevSampleWorkspaceRule();

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    //private DockerMachine.LocalBuilder dockerMachine = DockerMachine.localMachine();


    @Rule
    public DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/docker/docker-compose.yml")
            .build();

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
                arg.add("/bin/bash");
                arg.add("-c");
                arg.add("./changeJenkinsUrl.sh " + jenkinsPort);
                return arg;
            }
        };
        docker.exec(options, "accurev", arguments);

    }
    public DockerTest() throws IOException {

    }


    @Test
    public void dockerTest() throws Exception {

        //////////
        docker.containers().container("accurev");


        sampleWorkspace.init("localhost", "5050", "accurev_user", "docker");


    }
}
