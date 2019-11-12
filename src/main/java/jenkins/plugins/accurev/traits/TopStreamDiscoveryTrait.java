package jenkins.plugins.accurev.traits;

import hudson.Extension;
import hudson.plugins.accurev.extensions.impl.TopStreamDiscovery;
import jenkins.plugins.accurev.AccurevSCMBuilder;
import jenkins.plugins.accurev.AccurevSCMSource;
import jenkins.plugins.accurev.AccurevSCMSourceContext;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMBuilder;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class TopStreamDiscoveryTrait extends  AccurevSCMExtensionTrait<TopStreamDiscovery>{


    @DataBoundConstructor
    public TopStreamDiscoveryTrait(String topStream) {
        super(new TopStreamDiscovery());
        setTopStream(topStream);

    }



    public String getTopStream() {
        return this.getExtension().getName();
    }

    public void setTopStream(String topStream) {
        this.getExtension().setName(topStream);
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof AccurevSCMSourceContext) {
            AccurevSCMSourceContext<?, ?> ctx = (AccurevSCMSourceContext<?, ?>) context;
            ctx.topStream(getTopStream());
        }
    }

    @Symbol("accurevTopStreamDiscoveryTrait")
    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {
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
        public Class<? extends SCMSource> getSourceClass() {
            return AccurevSCMSource.class;
        }

        @Override
        public String getDisplayName() {
            return "Topstream to discover from";
        }
    }
}
