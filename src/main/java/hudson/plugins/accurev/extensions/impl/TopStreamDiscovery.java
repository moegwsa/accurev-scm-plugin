package hudson.plugins.accurev.extensions.impl;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import hudson.plugins.accurev.extensions.AccurevSCMExtensionDescriptor;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.commands.PopulateCommand;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class TopStreamDiscovery extends AccurevSCMExtension {

    private String name;

    @DataBoundConstructor
    public TopStreamDiscovery() {
    }

    public TopStreamDiscovery(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void decoratePopulateCommand(AccurevSCM scm, Run<?, ?> build, AccurevClient accurev, TaskListener listener, PopulateCommand cmd) throws IOException, InterruptedException {
        super.decoratePopulateCommand(scm, build, accurev, listener, cmd);
    }

    @Extension
    public static class DescriptorImpl extends AccurevSCMExtensionDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return "Choose a topstream";
        }
    }
}
