package jenkins.plugins.accurev.traits;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.plugins.accurev.extensions.impl.PathRestriction;
import jenkins.plugins.accurev.AccurevSCMBuilder;
import jenkins.plugins.accurev.AccurevSCMSource;
import jenkins.plugins.accurev.AccurevSCMSourceContext;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMBuilder;
import jenkins.scm.api.trait.SCMHeadPrefilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class PathRestrictionTrait extends AccurevSCMExtensionTrait<PathRestriction> {

    @DataBoundConstructor
    public PathRestrictionTrait(String included, String excluded) {
        super(new PathRestriction());
        getExtension().setIncludedRegions(included);
        getExtension().setExcludedRegions(excluded);
    }

    public String getIncluded() {
        return getExtension().getIncludedRegions();
    }

    public String getExcluded() {
        return getExtension().getExcludedRegions();
    }

    public Boolean isTransactionExcluded(AccurevTransaction transaction, TaskListener listener) throws IOException, InterruptedException { return getExtension().isTransactionExcluded(transaction, listener);}

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
//        if(context instanceof AccurevSCMSourceContext){
//            context.withPrefilter(new SCMHeadPrefilter() {
//                @Override
//                public boolean isExcluded(@NonNull SCMSource source, @NonNull SCMHead head) {
//                    if(source instanceof AccurevSCMSource){
//                        AccurevClient client = ((AccurevSCMSource) source).getAccurevClient();
//                        AccurevTransaction highest = client.fetchTransaction(head.getName());
//
//                        try {
//                            if(isTransactionExcluded(highest, ((AccurevSCMSource) source).getListener())) return true;
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    return false;
//                }
//            });
//        }
    }




    @Symbol("accurevExcludeTriggerFromFilesTrait")
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
            return "Ignore keep/promotes from certain paths";
        }
    }



}
