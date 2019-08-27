package hudson.plugins.accurev.extensions.impl;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.StreamSpec;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.AccurevException;
import jenkins.plugins.accurevclient.commands.PopulateCommand;

import java.nio.file.Paths;

/**
 * TODO: Add possibility of setting a custom name for workspace
 */
public class BuildWithWorkspace extends AccurevSCMExtension {

    boolean buildWithWorkspace = false;

    public void setBuildWithWorkspace(boolean buildWithWorkspace) {
        this.buildWithWorkspace = buildWithWorkspace;
    }

    @Override
    public void decoratePopulateCommand(AccurevSCM scm, Run<?, ?> build, AccurevClient accurev, TaskListener listener, PopulateCommand cmd) throws InterruptedException {
        StreamSpec streamSpec = scm.getStreams().get(0);
        if (!accurev.getInfo().getInWorkspace()) {
            try {
                accurev.workspace().create((streamSpec.getName() + "-jenkins"), streamSpec.getName()).execute();
            } catch (AccurevException e) {
                accurev.changeWS().name(build.getDisplayName()).location(Paths.get("").toAbsolutePath().toString());
            }
        } else {
            accurev.changeWS().name(build.getDisplayName()).location(Paths.get("").toAbsolutePath().toString());
        }
    }

    @Override
    public boolean requiresWorkspace() {
        return true;
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return true;
    }

    public boolean isBuildWithWorkspace() {
        return buildWithWorkspace;
    }
}
