package jenkins.plugins.accurev.traits;

import hudson.Extension;
import hudson.plugins.accurev.extensions.impl.BuildItemsDiscovery;
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

public class BuildItemsDiscoveryTrait extends AccurevSCMExtensionTrait<BuildItemsDiscovery> {

    @DataBoundConstructor
    public BuildItemsDiscoveryTrait(boolean stream, boolean workspace, boolean snapshot, boolean passThrough, boolean gatedStream) {
        super(new BuildItemsDiscovery());
        this.setStream(stream);
        this.setWorkspace(workspace);
        this.setSnapshot(snapshot);
        this.setPassThrough(passThrough);
        this.setGatedStream(gatedStream);
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof AccurevSCMSourceContext) {
            AccurevSCMSourceContext<?, ?> ctx = (AccurevSCMSourceContext<?, ?>) context;
            ctx.wantStreams(isStream());
            ctx.wantSnapshots(isSnapshot());
            ctx.wantWorkspaces(isWorkspace());
            ctx.wantPassThroughs(isPassThrough());
            ctx.wantGatedStreams(isGatedStream());
        }
    }

    public boolean isStream() {
        return this.getExtension().getStream();
    }

    public void setStream(boolean stream) {
        this.getExtension().setStream(stream);
    }

    public boolean isSnapshot() {
        return this.getExtension().isSnapshot();
    }

    public void setSnapshot(boolean snapshot) {
        this.getExtension().setSnapshot(snapshot);
    }

    public boolean isWorkspace() {
        return this.getExtension().isWorkspace();
    }

    public void setWorkspace(boolean workspace) {
        this.getExtension().setWorkspace(workspace);
    }

    public boolean isPassThrough() {
        return this.getExtension().isPassThrough();
    }

    public void setPassThrough(boolean passThrough) {
        this.getExtension().setPassThrough(passThrough);
    }

    public boolean isGatedStream() {
        return this.getExtension().isGatedStream();
    }

    public void setGatedStream(boolean gatedStream) { this.getExtension().setGatedStream(gatedStream);
    }

    @Symbol("accurevBuildItemsDiscoveryTrait")
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
            return "Types to discover";
        }
    }
}

