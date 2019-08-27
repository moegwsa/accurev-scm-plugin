package hudson.plugins.accurev;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StreamSpecTest {

    @Test
    public void testMatch() {
        StreamSpec l = new StreamSpec("stream", "depot");
        assertTrue(l.matches("stream", "depot"));
        assertFalse(l.matches("stream", "depot2"));
        assertFalse(l.matches("stream1","depot"));
    }
}
