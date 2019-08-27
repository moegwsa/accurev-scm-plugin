package hudson.plugins.accurev.util;

import hudson.model.Result;
import hudson.plugins.accurev.AccurevChangeSet;
import jenkins.plugins.accurevclient.model.AccurevStream;
import jenkins.plugins.accurevclient.model.AccurevStreamType;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import jenkins.plugins.accurevclient.model.TransactionType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Random;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class BuildDataTest {

    private BuildData data;
    private AccurevTransaction at;
    private AccurevStream as;

    @Before
    public void setUp() {
        data = new BuildData();
        at = new AccurevTransaction(2, "", TransactionType.Promote, new Date(), "", null, "test");
        as = new AccurevStream("test", "test", Long.valueOf(1), "", null, false, AccurevStreamType.Normal, new Date(), null);
    }

    @Test
    public void testGetDisplayName() {
        assertThat(data.getDisplayName(), is("Accurev Build Data"));
    }

    @Test
    public void testGetDisplayNameEmptyString() {
        String scmName = "";
        BuildData dataWithSCM = new BuildData(scmName);
        assertThat(dataWithSCM.getDisplayName(), is ("Accurev Build Data"));
    }

    @Test
    public void testGetDisplayNameWithSCMName() {
        final String scmName = "testSCM";
        final BuildData dataWithSCM = new BuildData(scmName);
        assertThat(dataWithSCM.getDisplayName(), is("Accurev Build Data: " + scmName));
    }

    @Test
    public void testGetIconFileName() {
        assertThat(data.getIconFileName(), endsWith("/plugin/accurev/icons/accurev-48x48.png"));
    }

    @Test
    public void testGetUrlName() {
        assertThat(data.getUrlName(), is("accurev"));
    }

    @Test
    public void testGetUrlNameMultipleEntries() {
        Random random = new Random();
        int randomIndex = random.nextInt(1234) + 1;
        data.setIndex(randomIndex);
        assertThat(data.getUrlName(), is("accurev-" + randomIndex));
    }

    @Test
    public void testHasBeenBuilt() {
        assertFalse(data.hasBeenBuilt(at, as));
    }

    @Test
    public void testGetLastBuild() {
        assertEquals(null, data.getLastBuild(at, as));
    }

    @Test
    public void testSaveBuild() {
        Build build = new Build(at, as, 1, Result.SUCCESS);
        data.saveBuild(build);
        Assert.assertThat(data.getLastBuild(at, as), is(build));
    }

    @Test
    public void testGetLastBuiltTransaction() {
        Build build = new Build(at,as, 1, Result.SUCCESS);
        data.saveBuild(build);
        assertThat(data.getLastBuiltTransaction(), is(at));
    }

    @Test
    public void testGetLastBuiltTransactionType() {
        Build build = new Build(at,as, 1, Result.SUCCESS);
        data.saveBuild(build);
        assertEquals(data.getLastBuiltTransaction().getType(), TransactionType.Promote);
    }

    @Test
    public void testGetScmName() {
        assertThat(data.getScmName(), is(""));
    }

    @Test
    public void testSetScmName() {
        final String scmName = "Some SCM name";
        data.setScmName(scmName);
        assertThat(data.getScmName(), is(scmName));
    }

    @Test
    public void testAddRemoteUrl() {
        data.addRemoteStream("test");
        assertEquals(1, data.getRemoteStreams().size());

        String remoteUrl2 = "test2";
        data.addRemoteStream(remoteUrl2);
        assertFalse(data.getRemoteStreams().isEmpty());
        assertTrue("Second stream found in remote streams", data.getRemoteStreams().contains(remoteUrl2));
        assertEquals(2, data.getRemoteStreams().size());
    }

    @Test
    public void testHasBeenReferenced() {
        assertFalse(data.hasBeenReferenced("test"));
        data.addRemoteStream("test");
        assertTrue(data.hasBeenReferenced("test"));
        assertFalse(data.hasBeenReferenced("test" + "/"));
    }

    @Test
    public void testToString() {
        assertEquals(data.toString(), data.clone().toString());
    }

    @Test
    public void testToStringEmptyBuildData() {
        BuildData empty = new BuildData();
        assertThat(empty.toString(), endsWith("[scmName=<null>,remoteStreams=[],lastBuild=null]"));
    }

    @Test
    public void testToStringNullSCMBuildData() {
        BuildData nullSCM = new BuildData(null);
        assertThat(nullSCM.toString(), endsWith("[scmName=<null>,remoteStreams=[],lastBuild=null]"));
    }

    @Test
    public void testToStringNonNullSCMBuildData() {
        BuildData nonNullSCM = new BuildData("accurevless");
        assertThat(nonNullSCM.toString(), endsWith("[scmName=accurevless,remoteStreams=[],lastBuild=null]"));
    }

    @Test
    public void testEquals() {
        // Null object not equal non-null
        BuildData nullData = null;
        assertFalse("Null object not equal non-null", data.equals(nullData));

        // Object should equal itself
        assertEquals("Object not equal itself", data, data);
        assertTrue("Object not equal itself", data.equals(data));
        assertEquals("Object hashCode not equal itself", data.hashCode(), data.hashCode());

        BuildData data1 = data.clone();
        assertEquals("Cloned objects not equal", data1, data);
        assertTrue("Cloned objects not equal", data1.equals(data));
        assertTrue("Cloned objects not equal", data.equals(data1));
        assertEquals("Cloned object hashCodes not equal", data.hashCode(), data1.hashCode());

        // Saved build makes object unequal

        Build build1 = new Build(at, as, 1, Result.SUCCESS);
        data1.saveBuild(build1);
        assertFalse("Distinct objects shouldn't be equal", data.equals(data1));
        assertFalse("Distinct objects shouldn't be equal", data1.equals(data));

        // Same saved build makes objects equal
        BuildData data2 = data.clone();
        data2.saveBuild(build1);
        assertTrue("Objects with same saved build not equal", data2.equals(data1));
        assertTrue("Objects with same saved build not equal", data1.equals(data2));
        assertEquals("Objects with same saved build not equal hashCodes", data2.hashCode(), data1.hashCode());

        // Add remote URL makes objects unequal
        final String remoteUrl2 = "test";
        data1.addRemoteStream(remoteUrl2);
        assertFalse("Distinct objects shouldn't be equal", data.equals(data1));
        assertFalse("Distinct objects shouldn't be equal", data1.equals(data));

        // Add same remote URL makes objects equal
        data2.addRemoteStream(remoteUrl2);
        assertTrue("Objects with same remote URL not equal", data2.equals(data1));
        assertTrue("Objects with same remote URL not equal", data1.equals(data2));
        assertEquals("Objects with same remote URL not equal hashCodes", data2.hashCode(), data1.hashCode());

        // Another saved build still keeps objects equal
        Build build2 = new Build(at, as, 1, Result.FAILURE);
        assertEquals(build1, build2); // Surprising, since build1 result is SUCCESS, build2 result is FAILURE
        data1.saveBuild(build2);
        data2.saveBuild(build2);
        assertTrue(data1.equals(data2));
        assertEquals(data1.hashCode(), data2.hashCode());

        // Saving different build results still equal BuildData,
        // because the different build results are equal
        data1.saveBuild(build1);
        data2.saveBuild(build2);
        assertTrue(data1.equals(data2));
        assertEquals(data1.hashCode(), data2.hashCode());

        // Set SCM name doesn't change equality or hashCode
        data1.setScmName("scm 1");
        assertTrue(data1.equals(data2));
        assertEquals(data1.hashCode(), data2.hashCode());
        data2.setScmName("scm 2");
        assertTrue(data1.equals(data2));
        assertEquals(data1.hashCode(), data2.hashCode());

        BuildData emptyData = new BuildData();
        emptyData.remoteStreams = null;
        assertNotEquals("Non-empty object equal empty", data, emptyData);
        assertNotEquals("Empty object similar to non-empty", emptyData, data);
    }

    @Test
    public void testSetIndex() {
        data.setIndex(null);
        assertEquals(null, data.getIndex());
        data.setIndex(-1);
        assertEquals(null, data.getIndex());
        data.setIndex(0);
        assertEquals(null, data.getIndex());
        data.setIndex(13);
        assertEquals(13, data.getIndex().intValue());
        data.setIndex(-1);
        assertEquals(null, data.getIndex());
    }

    @Test
    public void testGetIndex() {
        assertEquals(null, data.getIndex());
    }

    @Test
    public void testGetRemoteStreams() {
        assertTrue(data.getRemoteStreams().isEmpty());
    }

    @Test
    public void testHashCodeEmptyData() {
        BuildData emptyData = new BuildData();
        assertEquals(emptyData.hashCode(), emptyData.hashCode());
        emptyData.remoteStreams = null;
        assertEquals(emptyData.hashCode(), emptyData.hashCode());
    }
}
