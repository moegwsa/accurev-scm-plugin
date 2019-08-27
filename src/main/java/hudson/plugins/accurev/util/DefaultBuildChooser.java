package hudson.plugins.accurev.util;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.plugins.accurev.StreamSpec;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.model.AccurevStream;
import jenkins.plugins.accurevclient.model.AccurevStreamType;
import jenkins.plugins.accurevclient.model.AccurevStreams;
import jenkins.plugins.accurevclient.model.AccurevTransaction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class DefaultBuildChooser extends BuildChooser {

    @Override
    public Collection<AccurevTransaction> getCandidateTransactions(boolean isPollCall, String streamSpec, AccurevClient ac, TaskListener listener, BuildData data){
        if(!isPollCall) {

        }

        StreamSpec ss = null;
        for(StreamSpec as : accurevSCM.getStreams()){
            if(as.getName().equals(streamSpec)) ss = as;
        }
        try {
                StandardUsernamePasswordCredentials cred = ac.getCredentials();
            if(cred != null) {
                ac.login().username(cred.getUsername()).password(cred.getPassword()).execute();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Collection<AccurevTransaction> cAT = ac.getUpdatesFromAncestors(
                ss.getDepot(),
                ss.getName(),
                (data.lastBuild != null ? data.lastBuild.transaction.getId() : 0) // Compare to the Transaction ID of the last build we began.
        );
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
