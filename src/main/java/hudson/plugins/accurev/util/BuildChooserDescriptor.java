package hudson.plugins.accurev.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.Item;
import jenkins.model.Jenkins;

import java.util.logging.Logger;

public abstract class BuildChooserDescriptor extends Descriptor<BuildChooser> {
    private static final Logger LOGGER = Logger.getLogger(BuildChooserDescriptor.class.getName());
    /**
     * Before this extension point was formalized, existing {@link BuildChooser}s had
     * a hard-coded ID name used for the persistence.
     *
     * This method returns those legacy ID, if any, to keep compatibility with existing data.
     * @return legacy ID, if any, to keep compatibility with existing data.
     */
    public String getLegacyId() {
        return null;
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "Tests use null instance, Jenkins 2.60 declares instance is not null")
    public static DescriptorExtensionList<BuildChooser,BuildChooserDescriptor> all() {
        Jenkins jenkins = Jenkins.getInstance();
        return jenkins.getDescriptorList(BuildChooser.class);
    }

    /**
     * Returns true if this build chooser is applicable to the given job type.
     *
     * @param job the type of job or item.
     * @return true to allow user to select this build chooser.
     */
    public boolean isApplicable(java.lang.Class<? extends Item> job) {
        return true;
    }
}
