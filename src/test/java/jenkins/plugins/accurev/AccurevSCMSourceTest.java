package jenkins.plugins.accurev;

import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.plugins.accurev.AccurevStatus;
import jenkins.scm.api.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.is;

public class AccurevSCMSourceTest {


    public static final String REMOTEHOST = "localhost";
    public static final String REMOTEPORT = "8081";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public AccurevSampleWorkspaceRule sampleWorkspace = new AccurevSampleWorkspaceRule();

    private AccurevStatus accurevStatus;

    @Before
    public void setup() { accurevStatus = new AccurevStatus(); }

    @Test
    public void testSourceOwnerTriggeredByDoNotifyCommit() throws Exception {
        AccurevSCMSource accurevSCMSource = new AccurevSCMSource("id", REMOTEHOST + ":" +REMOTEPORT, "");
        AccurevSCMSourceOwner scmSourceOwner = setupAccurevSCMSourceOwner(accurevSCMSource);
        jenkins.getInstance().add(scmSourceOwner, "accurevSourceOwner");


        accurevStatus.doNotifyCommit(mock(HttpServletRequest.class), REMOTEHOST, REMOTEPORT, "master", "10", "testPrincipal");

        SCMHeadEvent event =
                jenkins.getInstance().getExtensionList(SCMEventListener.class).get(SCMEventListenerImpl.class)
                        .waitSCMHeadEvent(1, TimeUnit.SECONDS);
        assertThat(event, notNullValue());

        assertThat((Iterable<SCMHead>) event.heads(accurevSCMSource).keySet(), hasItem(is(new AccurevSCMHead("master"))));
        verify(scmSourceOwner, times(0)).onSCMSourceUpdated(accurevSCMSource);
    }

    private AccurevSCMSourceOwner setupAccurevSCMSourceOwner(AccurevSCMSource accurevSCMSource) {
        AccurevSCMSourceOwner owner = mock(AccurevSCMSourceOwner.class);
        when(owner.hasPermission(Item.READ)).thenReturn(true, true, true);
        when(owner.getSCMSources()).thenReturn(Collections.<SCMSource>singletonList(accurevSCMSource));
        return owner;
    }

    public interface AccurevSCMSourceOwner extends TopLevelItem, SCMSourceOwner {
    }

    @TestExtension
    public static class SCMEventListenerImpl extends SCMEventListener {

        SCMHeadEvent<?> head = null;

        @Override
        public void onSCMHeadEvent(SCMHeadEvent<?> event) {
            synchronized (this) {
                head = event;
                notifyAll();
            }
        }

        public SCMHeadEvent<?> waitSCMHeadEvent(long timeout, TimeUnit units)
                throws TimeoutException, InterruptedException {
            long giveUp = System.currentTimeMillis() + units.toMillis(timeout);
            while (System.currentTimeMillis() < giveUp) {
                synchronized (this) {
                    SCMHeadEvent h = head;
                    if (h != null) {
                        head = null;
                        return h;
                    }
                    wait(Math.max(1L, giveUp - System.currentTimeMillis()));
                }
            }
            throw new TimeoutException();
        }
    }
}
