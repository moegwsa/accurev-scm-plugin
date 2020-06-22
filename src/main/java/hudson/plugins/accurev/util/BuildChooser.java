package hudson.plugins.accurev.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.TaskListener;
import hudson.plugins.accurev.AccurevSCM;
import jenkins.model.Jenkins;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.model.AccurevTransaction;

import java.io.Serializable;
import java.util.Collection;

public abstract class BuildChooser implements ExtensionPoint, Describable<BuildChooser>, Serializable {

    public transient AccurevSCM accurevSCM;

    /**
            * Short-hand to get to the display name.
     * @return display name of this build chooser
     */
    public final String getDisplayName() {
        return getDescriptor().getDisplayName();
    }

    public Collection<AccurevTransaction> getCandidateTransactions(boolean isPollCall, String streamSpec, AccurevClient ac, TaskListener listener, BuildData data){
        throw new UnsupportedOperationException("getCandidateRevisions method must be overridden");
    }

    public Collection<AccurevTransaction> getCandidateTransactions(boolean isPollCall, String streamSpec, AccurevClient ac, TaskListener listener, BuildData data, AccurevTransaction bound){
        throw new UnsupportedOperationException("getCandidateRevisions method must be overridden");
    }

    @Override
    @SuppressFBWarnings(value="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification="Jenkins.getInstance() is not null")
    public BuildChooserDescriptor getDescriptor() {
        return (BuildChooserDescriptor)Jenkins.getInstanceOrNull().getDescriptorOrDie(getClass());
    }
}
