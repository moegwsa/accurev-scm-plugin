package hudson.plugins.accurev;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.accurev.util.BuildData;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class MqttResponseStep extends Notifier implements SimpleBuildStep{


    private final String url;

    @DataBoundConstructor
    public MqttResponseStep(final String url){
        this.url = url;
    }

    public String getUrl() {
        return url;
    }


    public void perform(@Nonnull final Run<?, ?> run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher, @Nonnull final TaskListener listener) throws InterruptedException, IOException {

        // TODO: Find a better way to access buildData
        BuildData buildData = run.getActions(BuildData.class).get(0);

        //BuildData buildData1 = (BuildData) run.getActions(BuildData.class).stream()
        //        .filter(item -> item.remoteStreams));

        // Using $RUN_DISPLAY_URL instead of $BUILD_URL due to blue ocean link.
        final String content = this.replaceVariables("$RUN_DISPLAY_URL", run, listener) + "\n"
                + this.replaceVariables("$BUILD_RESULT", run, listener) + "\n";


        // Todo: Need to create some security so that if the buildData is empty, we can catch the empty transaction in the perl script
        final String topic = "gatedStream/" + buildData.lastBuild.getMarked().getName() + "/" + buildData.lastBuild.transaction.getId();

        final int qualityOfService = 2;
        final String broker = "tcp://" + url;
        final String clientId = "Jenkins MQTT";
        final MemoryPersistence persistence = new MemoryPersistence();

        try {
            System.out.println("publishing message to broker: " + broker );
            final MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            final MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            listener.getLogger().println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            listener.getLogger().println("Connected");
            listener.getLogger().println("Publishing message: " + content);
            final MqttMessage message = new MqttMessage(content.getBytes("UTF-8"));
            message.setQos(qualityOfService);
            sampleClient.publish(topic, message);
            listener.getLogger().println("Message published");
            sampleClient.disconnect();
            listener.getLogger().println("Disconnected");
        } catch (final MqttException me) {
            listener.getLogger().println("reason " + me.getReasonCode());
            listener.getLogger().println("msg " + me.getMessage());
            listener.getLogger().println("loc " + me.getLocalizedMessage());
            listener.getLogger().println("cause " + me.getCause());
            listener.getLogger().println("excep " + me);
        }
    }

    private String replaceVariables(final String rawString, final Run<?, ?> run, final TaskListener listener)
            throws IOException, InterruptedException {

        final Result buildResult = run.getResult();
        final EnvVars env = run.getEnvironment(listener);
        if (run instanceof AbstractBuild) {
            env.overrideAll(((AbstractBuild) run).getBuildVariables());
        }

        // if buildResult is null, we might encounter bug https://issues.jenkins-ci.org/browse/JENKINS-46325
        env.put("BUILD_RESULT", buildResult != null ? buildResult.toString() : "");

        return env.expand(rawString);
    }

    @Override
    public boolean prebuild(final AbstractBuild<?, ?> build, final BuildListener listener) {
        return false;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        return false;
    }

    @Override
    public Action getProjectAction(final AbstractProject<?, ?> project) {
        return null;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> getProjectActions(final AbstractProject<?, ?> project) {
        return project.getActions();
    }

    @Symbol("mqttResponse")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}