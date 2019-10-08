package hudson.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.palantir.docker.compose.DockerComposeRule;
import hudson.model.*;
import hudson.util.StreamTaskListener;
import jenkins.plugins.accurev.AccurevSampleWorkspaceRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class AccurevSCMTest {

    @Rule
    public AccurevSampleWorkspaceRule sampleWorkspace = new AccurevSampleWorkspaceRule();

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Rule
    public DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/docker/docker-compose.yml")
            .build();

    private String host = "localhost";
    private String port = "5050";
    private String username = "accurev_user";
    private String password = "docker";

    protected TaskListener listener;

    @Before
    public void setUp() throws Exception {
        System.err.println("Starting set up");
        listener = StreamTaskListener.fromStderr();
        System.err.println("initializing workspace");
        sampleWorkspace.init(host, port, username, password);
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

        final String commitFile1 = "commitFile1";
        commit(commitFile1, username, commitFile1);
        System.err.println("Commited first file");
        final FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.err.println("Created freestyle build");
        rule.assertBuildStatus(Result.SUCCESS, build);
        assertFalse("scm polling should not detect any more changes after build", project.poll(listener).hasChanges());
        System.err.println("Build status asserted, true");
        final String commitFile2 = "commitFile2";
        commit(commitFile2, username, commitFile2);
        System.err.println("Commit second file");
        assertTrue("scm polling did not detect commit2 change", project.poll(listener).hasChanges());
        System.err.println("no pulling detected");
        final FreeStyleBuild build2 = project.scheduleBuild2(0).get();
        System.err.println("Create second build");
        final Set<User> culprits = build2.getCulprits();
        System.err.println("Get culprits");
        assertEquals("The build should have only one culprit", 1, culprits.size());
        assertEquals("", username, culprits.iterator().next().getFullName());
        assertTrue(build2.getWorkspace().child(commitFile2).exists());
        System.err.println("Assert culprits, username and file exists true");
        rule.assertBuildStatusSuccess(build2);
        assertFalse("scm polling should not detect any more changes after build", project.poll(listener).hasChanges());
    }

    private FreeStyleProject setupProject() throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        StandardUsernamePasswordCredentials dummyIdCredentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "accurev", "Accurev credentials", username, password);
        String depot = sampleWorkspace.mkDepot();
        String workspace = sampleWorkspace.mkWorkspace(depot);
        SystemCredentialsProvider.getInstance().getCredentials().add(dummyIdCredentials);

        AccurevSCM scm = new AccurevSCM(
                AccurevSCM.createDepotList(host, port,  "accurev"),
                Collections.singletonList(new StreamSpec(depot, depot)),
                null
                        );
        project.setScm(scm);
        return project;
    }

    private void commit(final String fileName, final String comitter, final String message) throws Exception {
        sampleWorkspace.commit(fileName, comitter, message);
    }
}
