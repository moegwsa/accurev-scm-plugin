package hudson.plugins.accurev;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import jenkins.triggers.SCMTriggerItem;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;


public class AccurevTrigger extends Trigger<Job<?, ?>> {

    public AccurevTrigger(String scmpoll_spec) throws ANTLRException {
        super(scmpoll_spec);
    }

    @DataBoundConstructor
    public AccurevTrigger() throws ANTLRException {
        super("");
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    @Symbol("accurevPush")
    public static class DescriptorImpl extends TriggerDescriptor {

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job && SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(item) != null;
        }

        @Override
        public String getDisplayName() {
            return "Accurev hook trigger for SCMPolling";
        }
    }
}

