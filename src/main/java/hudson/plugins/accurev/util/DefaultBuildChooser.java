package hudson.plugins.accurev.util;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.plugins.accurev.StreamSpec;
import jenkins.plugins.accurevclient.AccurevClient;

import jenkins.plugins.accurevclient.model.AccurevStreamType;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import jenkins.plugins.accurevclient.model.AccurevTransactions;

import java.util.Collection;

public class DefaultBuildChooser extends BuildChooser {

    @Override
    public Collection<AccurevTransaction> getCandidateTransactions(boolean isPollCall, String streamSpec, AccurevClient ac, TaskListener listener, BuildData data, long bound) {
        return updateCandidateTransactions(isPollCall, streamSpec, ac, listener, data, bound);
    }

    @Override
    public Collection<AccurevTransaction> getCandidateTransactions(boolean isPollCall, String streamSpec, AccurevClient ac, TaskListener listener, BuildData data){
        return updateCandidateTransactions(isPollCall, streamSpec, ac, listener, data, 0);
    }

    private Collection<AccurevTransaction> updateCandidateTransactions(boolean isPollCall, String streamSpec, AccurevClient ac, TaskListener listener, BuildData data, long bound){
        StreamSpec ss = null;
        for(StreamSpec as : accurevSCM.getStreams()){
            if(as.getName().equals(streamSpec)) ss = as;
        }

        Collection<AccurevTransaction> cAT;
        //Only look at changes since current transaction when building Staging Streams.

        if (ac.fetchStream(ss.getDepot(),ss.getName()).getType().equals(AccurevStreamType.Staging)){
            AccurevTransactions accurevTransactions = ac.getActiveTransactions(ss.getName());
            cAT = accurevTransactions.getTransactions();
        } else {
            long defaultBuild = 0;
            if(bound != 0) {
                cAT = ac.getUpdatesFromAncestors(
                        ss.getDepot(),
                        ss.getName(),
                        (data.lastBuild != null ? data.lastBuild.transaction.getId() : defaultBuild),
                        Long.toString(bound) // Compare to the Transaction ID of the last build we began.
                );
            } else {
                cAT = ac.getUpdatesFromAncestors(
                        ss.getDepot(),
                        ss.getName(),
                        (data.lastBuild != null ? data.lastBuild.transaction.getId() : defaultBuild)
                );
            }
        }

        if(!cAT.isEmpty()) listener.getLogger().println("New updates:");
        else listener.getLogger().println("No changes found");// TODO What if no changes happens, no new updates

        for(AccurevTransaction as : cAT){
            listener.getLogger().println("> Transaction id: " + as.getId() + ", comment: " + as.getComment() + ", user: " + as.getUser() + ", time: " + as.getTime());
        }

        return cAT;
    }

    @Extension
    public static final class DescriptorImpl extends BuildChooserDescriptor {
        @Override
        public String getDisplayName() {
            return "Build Chooser";
        }

        @Override
        public String getLegacyId() {
            return "Default";
        }
    }
}
