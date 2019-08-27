package hudson.plugins.accurev.extensions.impl;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import hudson.plugins.accurev.util.Build;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.commands.PopulateCommand;
import jenkins.plugins.accurevclient.model.AccurevStream;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collection;

public class TriggerChildren extends AccurevSCMExtension {

    private int depth;

    @DataBoundConstructor
    public TriggerChildren(int depth) {
        super();
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public void decoratePopulateCommand(AccurevSCM scm, Run<?, ?> build, AccurevClient accurev, TaskListener listener, PopulateCommand cmd) throws IOException, InterruptedException {

    }

    @Override
    public Collection<AccurevStream> getAffectedToBuild(AccurevSCM accurevSCM, Build transToBuild, AccurevClient ac) {
        return ac.getNDepthChildStreams(transToBuild.marked.getDepotName(), transToBuild.marked.getName(), depth);
    }
}
