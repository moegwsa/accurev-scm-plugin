package hudson.plugins.accurev.extensions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public class AccurevSCMExtensionDescriptor extends Descriptor<AccurevSCMExtension> {
    @SuppressFBWarnings(value="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification="Jenkins.getInstance() is not null")
    public static DescriptorExtensionList<AccurevSCMExtension,AccurevSCMExtensionDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(AccurevSCMExtension.class);
    }
}
