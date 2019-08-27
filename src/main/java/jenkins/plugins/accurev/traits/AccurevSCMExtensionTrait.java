package jenkins.plugins.accurev.traits;

import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import jenkins.plugins.accurev.AccurevSCMBuilder;
import jenkins.plugins.accurev.AccurevSCMSourceContext;
import jenkins.scm.api.trait.SCMBuilder;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.eclipse.jgit.annotations.NonNull;

public abstract class AccurevSCMExtensionTrait <E extends AccurevSCMExtension> extends SCMSourceTrait {
    private final E extension;

    public AccurevSCMExtensionTrait(@NonNull E extension) { this.extension = extension; }

    public E getExtension() {
        return extension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateBuilder(SCMBuilder<?,?> builder) {
        ((AccurevSCMBuilder<?>) builder).withExtension(extension);
    }


}
