package jenkins.plugins.accurev.traits;

import hudson.Extension;
import hudson.plugins.accurev.extensions.impl.MQTTStep;
import jenkins.plugins.accurev.AccurevSCMBuilder;
import jenkins.plugins.accurev.AccurevSCMSource;
import jenkins.plugins.accurev.AccurevSCMSourceContext;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMBuilder;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class MQTTStepTrait  extends AccurevSCMExtensionTrait<MQTTStep> {
    @DataBoundConstructor
    public MQTTStepTrait() {
        super(new MQTTStep());
    }



    @Symbol("accurevMQTTStepTrait")
    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {
        @Override
        public Class<? extends SCMBuilder> getBuilderClass() {
            return AccurevSCMBuilder.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return AccurevSCMSourceContext.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return AccurevSCMSource.class;
        }

        @Override
        public String getDisplayName() {
            return "Set Accurev Server url for Mosquitto";
        }
    }
}
