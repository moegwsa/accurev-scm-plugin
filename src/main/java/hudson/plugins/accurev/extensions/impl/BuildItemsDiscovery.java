package hudson.plugins.accurev.extensions.impl;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import hudson.plugins.accurev.extensions.AccurevSCMExtensionDescriptor;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.commands.PopulateCommand;
import org.apache.commons.lang.NotImplementedException;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class BuildItemsDiscovery extends AccurevSCMExtension {

    private boolean stream;
    private boolean snapshot;
    private boolean workspace;
    private boolean passThrough;
    private boolean stagingStream;

    @DataBoundConstructor
    public BuildItemsDiscovery() {
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public boolean getStream() {
        return stream;
    }

    public boolean isStream() {
        return stream;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public void setSnapshot(boolean snapshot) {
        this.snapshot = snapshot;
    }

    public boolean isWorkspace() {
        return workspace;
    }

    public void setWorkspace(boolean workspace) {
        this.workspace = workspace;
    }

    public boolean isPassThrough() {
        return passThrough;
    }

    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
    }

    public boolean isStagingStream() {
        return stagingStream;
    }

    public void setStagingStream(boolean stagingStream) {
        this.stagingStream = stagingStream;
    }

    @Override
    public void decoratePopulateCommand(AccurevSCM scm, Run<?, ?> build, AccurevClient accurev, TaskListener listener, PopulateCommand cmd) throws IOException, InterruptedException {

    }

    @Extension
    public static class DescriptorImpl extends AccurevSCMExtensionDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return "Choose types to discover";
        }
    }
}
