package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.github.dockerjava.api.DockerClient;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import hudson.plugins.accurev.util.AccurevTestExtensions;
import hudson.plugins.accurev.util.DockerUtils;
import jenkins.plugins.accurevclient.Accurev;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@Ignore
public class AccurevSCMFileSystemTest {

    // Depot: TestDepot1

    @ClassRule
    public static JenkinsRule rule = new JenkinsRule();

    String host = "localhost";
    String port = "5050";

    private AccurevClient client;
    private static DockerClient dockerClient = DockerUtils.getDockerClient();
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
                        DockerUtils.ContainerIsRunning(dockerClient,"accurev") &&
                        DockerUtils.ContainerExists(dockerClient,"accurev") &&
                        StringUtils.isNotBlank(url) &&
                        StringUtils.isNotBlank(username) &&
                        StringUtils.isNotEmpty(password)
        );
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        // Get the port from the JenkinsRule - When JenkinsRule runs it starts Jenkins at a random port
        String jenkinsPort = Integer.toString(rule.getURL().getPort());
        String[] arguments = {
                "perl",
                "./updateJenkinsHook.pl",
                jenkinsPort
        };

        DockerUtils.runCommand(dockerClient,"accurev",arguments);

        assertTrue(DockerUtils.readFileFromContainer(dockerClient,
                "accurev",
                "accurev/storage/site_slice/triggers/jenkinsConfig.JSON")
                .toString()
                .contains(jenkinsPort));

    }


    /** TODO: Have to find a way to test for content that isn't locally stored but has to be probed.
        currently the SCMFileSystem.children() gets every file from Accurev-SCM plugin dir and down
     */
//    @Test
//    public void mixedContent() throws Exception {
//        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
//        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
//                .addCredentials(Domain.global(), c);
//        sampleWorkspace.init(host, port, username, password);
//        String depot = sampleWorkspace.mkDepot();
//        sampleWorkspace.mkWorkspace(depot);
//        sampleWorkspace.commit("file", username, "modified");
//        sampleWorkspace.commit("file2", username, "new");
//        sampleWorkspace.commit("dir/file3", username, "new");
//        SCMSource source = new AccurevSCMSource(null, (host + ":" + port), c.getId());
//        SCMFileSystem fs = SCMFileSystem.of(source, new SCMHead(depot));
//        Assert.assertThat(fs, notNullValue());
//        Assert.assertThat(fs.getRoot(), notNullValue());
//        Iterable<SCMFile> children = fs.getRoot().children();
//        Set<String> names = new TreeSet<>();
//        SCMFile file = null;
//        SCMFile file2 = null;
//        SCMFile dir = null;
//        for (SCMFile f: children) {
//            names.add(f.getName());
//            switch (f.getName()) {
//                case "file":
//                    file = f;
//                    break;
//                case "file2":
//                    file2 = f;
//                    break;
//                case "dir":
//                    dir = f;
//                    break;
//                default:
//                    break;
//            }
//        }
//        assertThat(names, hasItems("file","file2","dir", "file3"));
//        assertThat(file.getType(), is(SCMFile.Type.REGULAR_FILE));
//        assertThat(file2.getType(), is(SCMFile.Type.REGULAR_FILE));
//        assertThat(dir.getType(), is(SCMFile.Type.DIRECTORY));
//        assertThat(file.contentAsString(), is("modified"));
//        assertThat(file2.contentAsString(), is("new"));
//    }
}
