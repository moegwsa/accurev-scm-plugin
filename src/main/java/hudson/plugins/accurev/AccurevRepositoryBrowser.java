package hudson.plugins.accurev;

import hudson.Util;
import hudson.model.Descriptor;
import hudson.plugins.accurev.browsers.AccurevWeb;
import hudson.scm.RepositoryBrowser;
import hudson.util.FormValidation;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


public abstract class AccurevRepositoryBrowser extends RepositoryBrowser<AccurevChangeSet> {

    public transient AccurevChangeSet current;

    private URL url;


    public AccurevRepositoryBrowser(String url) throws MalformedURLException {
        this.url = normalizeToEndWithSlash(new URL(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getChangeSetLink(AccurevChangeSet changeset) throws IOException {
        throw new UnsupportedOperationException("Method is not implemented for AccurevRepositoryBrowser");
    }

    /**
     * Returns a link to a specific revision of a file.
     *
     * @param path to a file.
     * @return URL pointing to a specific revision of the file.
     *
     * @throws MalformedURLException
     */
    public URL getFileLink(String path) throws MalformedURLException {
        throw new UnsupportedOperationException("Method is not implemented for AccurevRepositoryBrowser");
    }

    /**
     * Returns a link to a diff for a file.
     *
     * @param path to a file.
     * @return URL pointing to a specific revision of the file.
     *
     * @throws MalformedURLException
     */
    public URL getDiffLink(String path) throws MalformedURLException {
        throw new UnsupportedOperationException("Method is not implemented for AccurevRepositoryBrowser");
    }

    /**
     * Throws an {@link IllegalStateException} if current is null. This is used in subclasses.
     *
     * @throws IllegalStateException if current is null.
     */
    protected void checkCurrentIsNotNull() {
        if (current == null) {
            throw new IllegalStateException("current changeset must not be null, did you forget to call 'getChangeSetLink'?");
        }
    }



    public URL getUrl() {
        return url;
    }

    // compatibility with earlier plugins
    public Object readResolve() {
        if (!this.getClass().equals(AccurevRepositoryBrowser.class)) {
            return this;
        }
        // make sure to return the default HgWeb only if we no class is given in config.
        try {
            return new AccurevWeb(url.toExternalForm());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    protected static abstract class AccurevRepositoryBrowserDescriptor extends Descriptor<RepositoryBrowser<?>> {

        /**
         * Stapler does not seem to (reliably) find the web method on subclasses, so each needs to implement this to call {@link #_doCheckUrl}.
         * {@link #check} may be used for browser-specific checks.
         */
        public abstract FormValidation doCheckUrl(@QueryParameter String url);

        protected final FormValidation _doCheckUrl(@QueryParameter String url) {
            url = Util.fixNull(url);
            if (url.length() == 0) {
                return FormValidation.error("URL is mandatory");
            }
            try {
                URL u = new URL(url);
                if (u.getPath().endsWith("/")) {
                    return check(u);
                } else {
                    return FormValidation.warning("Browser URL should end with a slash (/)");
                }
            } catch (MalformedURLException x) {
                return FormValidation.error(x, "Invalid URL");
            }
        }

        /** Perform optional additional checks on URL, such as that it uses a particular host. */
        protected FormValidation check(URL url) {
            return FormValidation.ok();
        }

    }

}
