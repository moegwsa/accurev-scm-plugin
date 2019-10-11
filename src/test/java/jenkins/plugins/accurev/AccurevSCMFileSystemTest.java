package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.palantir.docker.compose.DockerComposeRule;
import hudson.plugins.accurev.util.AccurevTestExtensions;
import jenkins.plugins.accurevclient.Accurev;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import org.hamcrest.Matchers;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;


import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

@Ignore
public class AccurevSCMFileSystemTest {

    // Depot: TestDepot1

    @ClassRule
    public static JenkinsRule rule = new JenkinsRule();

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/docker/docker-compose.yml")
            .build();



    public static final String host = "localhost";
    public static final String port = "5050";
    public static final String username = "accurev_user";
    public static final String password = "docker";

    @Rule
    public AccurevSampleWorkspaceRule sampleWorkspace = new AccurevSampleWorkspaceRule();

    @Test
    public void createWorkspace() throws Exception {
        sampleWorkspace.init(host, port, username);
        sampleWorkspace.write("testFile", " ");
        //sampleWorkspace.accurev("add", "-c added testFile", "-x");
    }

    @BeforeClass
    public static void testAccurevInstall() throws IOException, InterruptedException {
        assumeTrue("Can only run test with proper test setup",
                AccurevTestExtensions.checkCommandExist("accurev")
        );
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
