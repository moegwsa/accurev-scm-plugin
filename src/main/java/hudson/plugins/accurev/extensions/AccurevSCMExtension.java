package hudson.plugins.accurev.extensions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.util.Build;
import hudson.plugins.accurev.util.BuildData;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.AccurevException;
import jenkins.plugins.accurevclient.commands.PopulateCommand;
import jenkins.plugins.accurevclient.model.AccurevStream;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import org.apache.commons.lang.NotImplementedException;


import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public abstract class AccurevSCMExtension extends AbstractDescribableImpl<AccurevSCMExtension>{

    public void decoratePopulateCommand(AccurevSCM scm, Run<?, ?> build,  AccurevClient accurev, TaskListener listener, PopulateCommand cmd) throws IOException, InterruptedException {
        throw new NotImplementedException();
    }

    @Override
    public AccurevSCMExtensionDescriptor getDescriptor() {
        return (AccurevSCMExtensionDescriptor) super.getDescriptor();
    }

    public boolean requiresWorkspaceForPolling() {
        return false;
    }

    public boolean requiresWorkspace() { return false; }

    @SuppressFBWarnings(value="NP_BOOLEAN_RETURN_NULL", justification="null used to indicate other extensions should decide")
    @CheckForNull
    public Boolean isTransactionExcluded(AccurevSCM scm, AccurevClient accurevClient, AccurevTransaction transaction, TaskListener listener, BuildData buildData) throws IOException, InterruptedException, AccurevException {
        return null;
    }

    public Collection<AccurevStream> getAffectedToBuild(AccurevSCM accurevSCM, Build transToBuild, AccurevClient ac) {
        return Collections.emptyList();
    }
}