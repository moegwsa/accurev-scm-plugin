package hudson.plugins.accurev.extensions.impl;

import hudson.Extension;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import hudson.plugins.accurev.extensions.AccurevSCMExtensionDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

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
