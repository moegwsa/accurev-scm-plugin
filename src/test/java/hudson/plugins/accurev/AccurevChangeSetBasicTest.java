package hudson.plugins.accurev;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class AccurevChangeSetBasicTest {

    @Test
    public void testChangeSet() {
        AccurevChangeSet accurevChangeSet = AccurevChangeSetUtil.genChangeSet();
        assertEquals(AccurevChangeSetUtil.TRANSACTION_ID, accurevChangeSet.getId());
    }

    @Test
    public void testCommitter() {
       assertEquals(AccurevChangeSetUtil.COMMITTER_NAME, AccurevChangeSetUtil.genChangeSet().getUser());
    }

    @Test
    public void testStreamName() {
        assertEquals(AccurevChangeSetUtil.STREAM_NAME, AccurevChangeSetUtil.genChangeSet().getStream());
    }

    @Test
    public void testGetDate() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        Date parsedDate = dateFormat.parse(AccurevChangeSetUtil.COMMITTER_DATE);
        assertEquals(parsedDate.getTime(), AccurevChangeSetUtil.genChangeSet().getTimestamp());
    }

    @Test
    public void testTransactionType() {
        assertEquals(AccurevChangeSetUtil.TRANSACTION_TYPE, AccurevChangeSetUtil.genChangeSet().getType());
    }
}
