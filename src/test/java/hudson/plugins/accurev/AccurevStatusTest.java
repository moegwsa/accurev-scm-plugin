package hudson.plugins.accurev;

import hudson.model.FreeStyleProject;
import hudson.triggers.SCMTrigger;
import jenkins.plugins.accurevclient.Accurev;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Matchers;
import org.mockito.MockingDetails;
import org.mockito.Mockito;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;


public class AccurevStatusTest {

    private AccurevStatus accurevStatus;
    private HttpServletRequest requestWithNoParameters;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        this.accurevStatus = new AccurevStatus();
        this.requestWithNoParameters = mock(HttpServletRequest.class);
    }

    @WithoutJenkins
    @Test
    public void testGetDisplayName() { assertEquals("Accurev", this.accurevStatus.getDisplayName());}

    @WithoutJenkins
    @Test
    public void testGetIconFileName() { assertNull(this.accurevStatus.getIconFileName());}

    @WithoutJenkins
    @Test
    public void testGetUrlName() { assertEquals("accurev", this.accurevStatus.getUrlName());}

    @WithoutJenkins
    @Test
    public void testToString() { assertEquals("HOST:  PORT: ", this.accurevStatus.toString());}

    @Test
    public void testDoNotifyCommitWithNoStreams() throws Exception {
        
        AccurevTrigger aMasterTrigger = setupProjectWithTrigger("host", "8080", "", "depot", false);
        HttpResponse httpResponse= this.accurevStatus.doNotifyCommit(requestWithNoParameters, "host", "8080", "", null, "testPrincipal");

        assertEquals("HOST: host PORT: 8080 Streams: ", this.accurevStatus.toString());
    }

    @Test
    public void testDoNotifyCommitWithSingleStream() throws Exception {
        // TODO : Find a way to mock the triggers
        AccurevTrigger aMasterTrigger = setupProjectWithTrigger("host", "8080", "stream1", "depot", false);

        AccurevStatus spy = Mockito.spy(new AccurevStatus());
        spy.doNotifyCommit(requestWithNoParameters, "host", "8080", "stream1", "1", "testPrincipal");

        Mockito.verify(spy).doNotifyCommit(requestWithNoParameters, "host", "8080", "stream1", "1", "testPrincipal");
        assertEquals("HOST: host PORT: 8080 Streams: stream1", spy.toString());
    }


    @Test
    public void testDoNotifyCommitWithTwoStreams() throws Exception {
        // TODO : Find a way to mock the triggers
        AccurevTrigger bMasterTrigger = setupProjectWithTrigger("host", "8080", "stream2", "depot", false);
        AccurevTrigger aMasterTrigger = setupProjectWithTrigger("host", "8080", "stream1", "depot", false);


        AccurevStatus spy = Mockito.spy(new AccurevStatus());

        spy.doNotifyCommit(requestWithNoParameters, "host", "8080", "stream1", "1", "testPrincipal");


        Mockito.verify(spy).doNotifyCommit(requestWithNoParameters, "host", "8080", "stream1", "1", "testPrincipal");

        assertEquals("HOST: host PORT: 8080 Streams: stream1", spy.toString());
    }

    @Test
    public void testDoNotifyCommitWitMultipleStreams() throws Exception {
        // TODO : Find a way to mock the triggers
        AccurevTrigger aMasterTrigger = setupProjectWithTrigger("host", "8080", "stream1", "depot", false);
        AccurevTrigger bMasterTrigger = setupProjectWithTrigger("host", "8080", "stream2", "depot", false);
        AccurevTrigger cMasterTrigger = setupProjectWithTrigger("host", "8080", "stream3", "depot", false);

        AccurevStatus spy = Mockito.spy(new AccurevStatus());
        spy.doNotifyCommit(requestWithNoParameters, "host", "8080", "stream1,stream2", "1", "testPrincipal");


        Mockito.verify(spy).doNotifyCommit(requestWithNoParameters, "host", "8080", "stream1,stream2", "1", "testPrincipal");

        assertEquals("HOST: host PORT: 8080 Streams: stream1,stream2", spy.toString());
    }

    private AccurevTrigger setupProjectWithTrigger(String host, String port, String streamString, String depotString, boolean ignoreNotifyCommit) throws Exception {
        AccurevTrigger trigger = Mockito.mock(AccurevTrigger.class);
        //Mockito.doReturn(ignoreNotifyCommit);
        setupProject(host, port, streamString, depotString, trigger);
        return trigger;
    }

    private void setupProject(String host, String port, String streamString, String depotString, AccurevTrigger trigger) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        List<StreamSpec> streams = new ArrayList<>();
        for (String stream : streamString.split(",")) {
            streams.add(new StreamSpec(stream, depotString));
        }

        AccurevSCM accurev = new AccurevSCM(
                Collections.singletonList(new ServerRemoteConfig(host, port, null)),
                streams,
                null
        );
        project.setScm(accurev);
        if (trigger != null) project.addTrigger(trigger);

    }

}
