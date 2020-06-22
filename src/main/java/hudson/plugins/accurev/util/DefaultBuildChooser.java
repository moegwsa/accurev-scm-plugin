package hudson.plugins.accurev.util;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.plugins.accurev.StreamSpec;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.model.AccurevStream;
import jenkins.plugins.accurevclient.model.AccurevStreamType;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import jenkins.plugins.accurevclient.model.AccurevTransactions;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

public class DefaultBuildChooser extends BuildChooser {

    @Override
    public Collection<AccurevTransaction> getCandidateTransactions(boolean isPollCall, String streamSpec, AccurevClient ac, TaskListener listener, BuildData data, AccurevTransaction bound) {
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
        Collection<AccurevTransaction> cAT;
        //Only look at changes since current transaction when building Staging Streams.
        listener.getLogger().println(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()) + " Calculate Accurev candidate transactions");
        if (ac.fetchStream(ss.getDepot(),ss.getName()).getType().equals(AccurevStreamType.Staging)){
            AccurevTransactions accurevTransactions = ac.getActiveTransactions(ss.getName());
            cAT = accurevTransactions.getTransactions();
        } else {
            long defaultBuild = 0;
            listener.getLogger().println(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()) + " Fetch updates from ancestors");
            cAT = ac.getUpdatesFromAncestors(
                    ss.getDepot(),
                    ss.getName(),
                    (data.lastBuild != null ? data.lastBuild.transaction.getId() : defaultBuild),
                    Long.toString(bound.getId())// Compare to the Transaction ID of the last build we began.
            );
            listener.getLogger().println(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()) + " Ancestors updated");
        }
        if(!cAT.isEmpty()) listener.getLogger().println("New updates:");
        else listener.getLogger().println("No changes found");// TODO What if no changes happens, no new updates

        for(AccurevTransaction as : cAT){
            listener.getLogger().println("> Transaction id: " + as.getId() + ", comment: " + as.getComment() + ", user: " + as.getUser() + ", time: " + as.getTime());
        }
        listener.getLogger().println(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()) + " return changelog");
        return cAT;
    }

    @Override
    public Collection<AccurevTransaction> getCandidateTransactions(boolean isPollCall, String streamSpec, AccurevClient ac, TaskListener listener, BuildData data){

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
        Collection<AccurevTransaction> cAT;
        //Only look at changes since current transaction when building Staging Streams.
        listener.getLogger().println(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()) + " Calculate Accurev candidate transactions");
        if (ac.fetchStream(ss.getDepot(),ss.getName()).getType().equals(AccurevStreamType.Staging)){
            AccurevTransactions accurevTransactions = ac.getActiveTransactions(ss.getName());
            cAT = accurevTransactions.getTransactions();
        } else {
            long defaultBuild = 0;
            listener.getLogger().println(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()) + " Fetch updates from ancestors");
            cAT = ac.getUpdatesFromAncestors(
                    ss.getDepot(),
                    ss.getName(),
                    (data.lastBuild != null ? data.lastBuild.transaction.getId() : defaultBuild) // Compare to the Transaction ID of the last build we began.
            );
            listener.getLogger().println(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()) + " Ancestors updated");
        }
        if(!cAT.isEmpty()) listener.getLogger().println("New updates:");
        else listener.getLogger().println("No changes found");// TODO What if no changes happens, no new updates

        for(AccurevTransaction as : cAT){
            listener.getLogger().println("> Transaction id: " + as.getId() + ", comment: " + as.getComment() + ", user: " + as.getUser() + ", time: " + as.getTime());
        }
        listener.getLogger().println(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()) + " return changelog");
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
