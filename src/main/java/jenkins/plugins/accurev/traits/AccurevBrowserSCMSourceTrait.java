package jenkins.plugins.accurev.traits;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.accurev.AccurevRepositoryBrowser;
import hudson.plugins.accurev.AccurevSCM;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import jenkins.plugins.accurev.AccurevSCMBuilder;
import jenkins.plugins.accurev.AccurevSCMSource;
import jenkins.plugins.accurev.AccurevSCMSourceContext;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMBuilder;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.util.List;

public class AccurevBrowserSCMSourceTrait extends SCMSourceTrait {
    /**
     * The configured {@link AccurevRepositoryBrowser} or {@code null} to use the "auto" browser.
     */
    @CheckForNull
    private final AccurevRepositoryBrowser browser;

    /**
     * Stapler constructor.
     *
     * @param browser the {@link AccurevRepositoryBrowser} or {@code null} to use the "auto" browser.
     */
    @DataBoundConstructor
    public AccurevBrowserSCMSourceTrait(@CheckForNull AccurevRepositoryBrowser browser) {
        this.browser = browser;
    }

    /**
     * Gets the {@link AccurevRepositoryBrowser}..
     *
     * @return the {@link AccurevRepositoryBrowser} or {@code null} to use the "auto" browser.
     */
    @CheckForNull
    public AccurevRepositoryBrowser getBrowser() {
        return browser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateBuilder(SCMBuilder<?, ?> builder) {
        ((AccurevSCMBuilder<?>) builder).withBrowser(browser);
    }

    /**
     * Our {@link hudson.model.Descriptor}
     */
    @Extension
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return "Configure Repository Browser";
        }

        /**
         * Expose the {@link AccurevRepositoryBrowser} instances to stapler.
         *
         * @return the {@link AccurevRepositoryBrowser} instances
         */
        @Restricted(NoExternalUse.class) // stapler
        public List<Descriptor<RepositoryBrowser<?>>> getBrowserDescriptors() {
            AccurevSCM.DescriptorImpl descriptor = (AccurevSCM.DescriptorImpl) Jenkins.get().getDescriptor(AccurevSCM.class);
            if (descriptor == null) {
                return java.util.Collections.emptyList(); // Should be unreachable
            }
            return descriptor.getBrowserDescriptors();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMBuilder> getBuilderClass() {
            return AccurevSCMBuilder.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return AccurevSCMSourceContext.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCM> getScmClass() {
            return AccurevSCM.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return AccurevSCMSource.class;
        }
    }
}
