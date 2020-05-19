package hudson.plugins.accurev.browser;

import hudson.plugins.accurev.AccurevChangeSet;
import hudson.plugins.accurev.AccurevRepositoryBrowser;
import jenkins.plugins.accurevclient.Accurev;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static hudson.plugins.accurev.AccurevChangeSetUtil.genChangeSet;
import static org.junit.Assert.assertEquals;

public abstract class AbstractBrowserTestBase {

    protected final AccurevRepositoryBrowser browser;
    protected final AccurevChangeSet changeSet;


    @SuppressWarnings("deprecation")
    public AbstractBrowserTestBase(AccurevRepositoryBrowser browser) {
        this.browser = browser;
        changeSet = genChangeSet();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetFileLinkIllegalState() throws IOException {
        browser.getFileLink("src/main/java/hudson/plugins/accurev/AccurevRepositoryBrowser.java");
    }

    @Test(expected = IllegalStateException.class)
    public void testGetDiffLinkIllegalState() throws IOException {
        browser.getDiffLink("src/main/java/hudson/plugins/accurev/AccurevRepositoryBrowser.java");
    }

    /**
     * @param expected
     * @throws IOException
     */
    protected void testGetFileLink(final String expected) throws IOException {
        browser.getChangeSetLink(changeSet);
        assertEquals(expected, browser.getFileLink("src/main/java/hudson/plugins/accurev/AccurevRepositoryBrowser.java").toExternalForm());
    }

    /**
     * @param expected
     * @throws IOException
     */
    protected void testGetDiffLink(final String expected) throws IOException {
        browser.getChangeSetLink(changeSet);
        assertEquals(expected, browser.getDiffLink("src/main/java/hudson/plugins/accurev/AccurevRepositoryBrowser.java").toExternalForm());
    }

    /**
     * @param expected
     * @throws IOException
     */
    protected void testGetChangeSetLinkAccurevChangeSet(final String expected) throws IOException {
        assertEquals(expected, browser.getChangeSetLink(changeSet).toExternalForm());
    }
}
