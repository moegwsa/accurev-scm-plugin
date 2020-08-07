package hudson.plugins.accurev.browser;

import hudson.plugins.accurev.browsers.AccurevWeb;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;

public class AccurevWebTest extends AbstractBrowserTestBase{

    private static final String REPO_URL = "https://localhost:8080/accurev/";

    public AccurevWebTest() throws MalformedURLException {

        super(new AccurevWeb(REPO_URL));
    }

    @Test
    public void testGetChangeSetLinkAccurevChangeSet() throws IOException {
        testGetChangeSetLinkAccurevChangeSet(REPO_URL +"stream/dev_testing_staged_stream_12/405612?view=trans_hist");
    }

    @Test
    public void testGetFileLink() throws IOException {
        testGetFileLink(REPO_URL + "webgui.jsp/browse/dev_testing_staged_stream_12/src/main/java/hudson/plugins/accurev/AccurevRepositoryBrowser.java?view=content" );
    }

//    @Test
//    public void testGetDiffLink() throws IOException {
//        testGetDiffLink(REPO_URL +"webgui.jsp?tran_number=405612&depot=dev_testing_staged_stream_12&view=trans_hist");
//    }
}
