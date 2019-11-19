package hudson.plugins.accurev.extensions.impl;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.commands.PopulateCommand;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class MQTTStep extends AccurevSCMExtension {

    @DataBoundConstructor
    public MQTTStep(){
    }

    @Override
    public void decoratePopulateCommand(AccurevSCM scm, Run<?, ?> build, AccurevClient accurev, TaskListener listener, PopulateCommand cmd) throws IOException, InterruptedException {
        final EnvVars env = build.getEnvironment(listener);
        env.put("ACCUREV_URL", scm.getServerRemoteConfigs().get(0).getUrl());
    }
}
