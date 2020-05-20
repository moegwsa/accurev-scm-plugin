package jenkins.plugins.accurev;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.plugins.accurev.AccurevRepositoryBrowser;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.ServerRemoteConfig;
import hudson.plugins.accurev.StreamSpec;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import hudson.scm.SCM;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.Collections;

public final class AccurevStep extends SCMStep {

    private String depot;
    private String credentialsId;
    private String stream;
    private String host;
    private String port;

    public AccurevStep(String depot, String port) {
        this.host = depot;
        this.port = port;
    }
    @DataBoundConstructor
    public AccurevStep(String host, String port, String depot, String stream, String credentialsId) {
        this.depot = depot;
        this.credentialsId = credentialsId;
        this.stream = stream;
        this.host = host;
        this.port = port;
    }

    public String getStream(){ return stream; }

    public String getCredentialsId() { return credentialsId; }

    @DataBoundSetter
    public void setStream(String stream) { this.stream = stream;}

    @DataBoundSetter
    public void setDepot(String depot) { this.depot = depot;}

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    public String getDepot() {
        return depot;
    }

    public String getHost() {
        return host;
    }

    @DataBoundSetter
    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    @DataBoundSetter
    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public SCM createSCM() {
        return new AccurevSCM(
                AccurevSCM.createDepotList(host, port, credentialsId),
                Collections.singletonList(new StreamSpec(stream, depot)),
                Collections.<AccurevSCMExtension>emptyList(),
                null);
    }

    @Override
    public void setPoll(boolean poll) {
        super.setPoll(true);
    }

    @Symbol("accurev")
    @Extension
    public static final class DescriptorImpl extends SCMStepDescriptor {
        @Inject
        private ServerRemoteConfig.DescriptorImpl delegate;

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
                                                     @QueryParameter String credentialsId) {
            return delegate.doFillCredentialsIdItems(project, credentialsId);
        }

        @Override
        public String getFunctionName() {
            return "accurev";
        }

        @Override
        public String getDisplayName(){ return "AccurevStep";}
    }
}

