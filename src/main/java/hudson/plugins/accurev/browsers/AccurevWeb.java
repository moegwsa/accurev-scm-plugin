package hudson.plugins.accurev.browsers;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.accurev.AccurevChangeSet;
import hudson.plugins.accurev.AccurevRepositoryBrowser;
import hudson.scm.RepositoryBrowser;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class AccurevWeb extends AccurevRepositoryBrowser {
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();



    @DataBoundConstructor
    public AccurevWeb(String url) throws MalformedURLException {
        super(url);
    }

    public Descriptor<RepositoryBrowser<?>> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public URL getDiffLink(String path) throws MalformedURLException {
        checkCurrentIsNotNull();
        // https://localhost:8080/accurev/WEBGUI.jsp?tran_number=7&depot=test&view=trans_hist
        return new URL(getUrl(), "webgui.jsp?tran_number=" +
                current.getId() +
                "&depot=" +
                current.getStream() +
                "&view=trans_hist");
    }

    @Override
    public URL getChangeSetLink(AccurevChangeSet accurevChangeSet) throws IOException {
        // https://localhost:8080/accurev/WebGui.jsp?view=issue&depot=test&issueNum=59
        current = accurevChangeSet;
        return new URL(getUrl(),"");
    }

    @Override
    public URL getFileLink(String path) throws MalformedURLException {
        checkCurrentIsNotNull();
        // https://localhost:8080/accurev/webgui/browse/test/test/java/doc/MainApp.html?view=content
        return new URL(getUrl(),"webgui.jsp/browse/"
                + current.getStream() +
                "/" +
                path +
                "?view=content");
    }


    @Extension
    public static final class DescriptorImpl extends AccurevRepositoryBrowserDescriptor {

        public String getDisplayName() {
            return "AccurevWeb";
        }

        @Override
        public FormValidation doCheckUrl(@QueryParameter String url) { return _doCheckUrl(url); }

        @Override
        public AccurevWeb newInstance(StaplerRequest req, @NonNull JSONObject jsonObject) throws FormException {
            assert req != null; //see inherited javadoc
            return req.bindJSON(AccurevWeb.class, jsonObject);
        }
    }
    private static final long serialVersionUID = 1L;

}
