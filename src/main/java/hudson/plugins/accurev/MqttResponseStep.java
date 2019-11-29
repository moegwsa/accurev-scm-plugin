package hudson.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.plugins.accurev.util.BuildData;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.plugins.accurevclient.AccurevClient;
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


public class MqttResponseStep extends Notifier implements SimpleBuildStep{


    private final String url;
    private final String credentialsId;

    @DataBoundConstructor
    public MqttResponseStep(String url, String credentials){
        this.url = url;
        this.credentialsId = credentials;
    }

    public String getUrl() {
        return url;
    }


    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        // TODO: Find a better way to access buildData
        BuildData buildData = null;
        for(Action a : run.getAllActions()) {
            if (a instanceof BuildData) buildData = (BuildData) a;
        }

        StandardUsernamePasswordCredentials usernamePasswordFromCredentialId = getUsernamePasswordFromCredentialId(run);

        String content;
            content = this.replaceVariables("$BUILD_URL", run, listener) + "\n"
                    + this.replaceVariables("$BUILD_RESULT", run, listener) + "\n"
                    + usernamePasswordFromCredentialId.getUsername() + "\n"
                    + usernamePasswordFromCredentialId.getPassword().getPlainText();

        // Todo: Need to create some security so that if the buildData is empty, we can catch the empty transaction in the perl script
        String topic = "gatedStream/" + buildData.lastBuild.getMarked().getName() + "/" + buildData.lastBuild.transaction.getId();
        
        int qualityOfService = 2;
        String broker = "tcp://" + url;
        String clientId = "Jenkins MQTT";
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            listener.getLogger().println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            listener.getLogger().println("Connected");
            listener.getLogger().println("Publishing message: " + content);
            MqttMessage message = new MqttMessage(content.getBytes("UTF-8"));
            message.setQos(qualityOfService);
            sampleClient.publish(topic, message);
            listener.getLogger().println("Message published");
            sampleClient.disconnect();
            listener.getLogger().println("Disconnected");
        } catch (MqttException me) {
            listener.getLogger().println("reason " + me.getReasonCode());
            listener.getLogger().println("msg " + me.getMessage());
            listener.getLogger().println("loc " + me.getLocalizedMessage());
            listener.getLogger().println("cause " + me.getCause());
            listener.getLogger().println("excep " + me);
        }
    }

    private StandardUsernamePasswordCredentials getUsernamePasswordFromCredentialId(Run<?, ?> build) {
        List<StandardUsernamePasswordCredentials> serverCredentials = CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class,
                build.getParent(),
                build.getParent() instanceof Queue.Task
                        ? Tasks.getAuthenticationOf((Queue.Task)build.getParent())
                        : ACL.SYSTEM,
                URIRequirementBuilder.fromUri("").build()
        );
        CredentialsMatcher srcMatcher = CredentialsMatchers.withUsername(credentialsId);
        CredentialsMatcher idMatcher = CredentialsMatchers.allOf(srcMatcher, AccurevClient.Companion.getCREDENTIALS_MATCHER());
        return CredentialsMatchers.firstOrNull(serverCredentials, idMatcher);
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
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return false;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return false;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return null;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        return project.getActions();
    }

    @Symbol("mqttResponse")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
