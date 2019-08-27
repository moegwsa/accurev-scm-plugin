package hudson.plugins.accurev;

import hudson.plugins.accurev.util.DefaultBuildChooser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class AccurevSCMUnitTest {

    private final String accurevDir = ".";
    private final AccurevSCM accurevSCM = new AccurevSCM(accurevDir);

    @Test
    public void testCreateDepotList() {
        String host = "";
        String port = "";
        String credentialsId = "";
        List<ServerRemoteConfig> expectedRemoteConfigList = new ArrayList<>();
        ServerRemoteConfig remoteConfig = new ServerRemoteConfig(host, port, credentialsId);
        expectedRemoteConfigList.add(remoteConfig);
        List<ServerRemoteConfig> remoteConfigList = AccurevSCM.createDepotList(host, port, credentialsId);
        assertEquals(expectedRemoteConfigList, remoteConfigList);
    }

    @Test
    public void testGetBuildChooser() {
        assertThat(accurevSCM.getBuildChooser(), is(instanceOf(DefaultBuildChooser.class)));
    }

    @Test
    public void testRequiresWorkspaceForPolling() {
        /* Assumes workspace is required */
        assertFalse(accurevSCM.requiresWorkspaceForPolling());
    }

    @Test
    public void testCreateChangeLogParser() {
        assertThat(accurevSCM.createChangeLogParser(), is(instanceOf(AccurevChangeLogParser.class)));
    }
}
