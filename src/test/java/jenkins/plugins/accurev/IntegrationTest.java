package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.StreamSpec;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import hudson.security.csrf.DefaultCrumbIssuer;
import hudson.triggers.SCMTrigger;
import hudson.util.OneShotEvent;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.TestBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IntegrationTest {
    @Rule
    public AccurevSampleWorkspaceRule sampleWorkspace = new AccurevSampleWorkspaceRule();

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Rule
    public DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/docker/docker-compose.yml")
            .build();

    private String jenkinsPort;

    @Before
    public void setUp() throws IOException, InterruptedException {
        // Get the port from the JenkinsRule - When JenkinsRule runs it starts Jenkins at a random port
        jenkinsPort = Integer.toString(rule.getURL().getPort());
        // For docker.exec command, no options needed.
        DockerComposeExecOption options = new DockerComposeExecOption() {
            @Override
            public List<String> options() {
                return new ArrayList<>();
            }
        };
        // Exec into the container, updating the url pointing to Jenkins with the correct port
        DockerComposeExecArgument arguments = new DockerComposeExecArgument() {
            @Override
            public List<String> arguments() {
                List<String> arg = new ArrayList<>();
                arg.add("perl");
                arg.add("updateJenkinsHook.pl");
                arg.add(jenkinsPort);
                return arg;
            }
        };

        docker.exec(options, "accurev", arguments);

        // For docker.exec command, no options needed.
        options = new DockerComposeExecOption() {
            @Override
            public List<String> options() {
                return new ArrayList<>();
            }
        };
        // Exec into the container, updating the url pointing to Jenkins with the correct port
        arguments = new DockerComposeExecArgument() {
            @Override
            public List<String> arguments() {
                List<String> arg = new ArrayList<>();
                arg.add("cat");
                arg.add("accurev/storage/site_slice/triggers/jenkinsConfig.JSON");
                return arg;
            }
        };
        assertTrue((docker.exec(options, "accurev", arguments).contains(jenkinsPort)));

    }


    @Test
    public void testWebhookConnection() throws Exception {
        // Initialize workspace
        sampleWorkspace.init("localhost", "5050", "accurev_user", "docker");
        // Creates depot and stream
        String depot = sampleWorkspace.mkDepot();
        // Create a workspace so we can promote a File
        sampleWorkspace.mkWorkspace(depot);

        FreeStyleProject project = rule.getInstance().createProject(FreeStyleProject.class, "test");
        SCMTrigger trigger = new SCMTrigger("");
        project.addTrigger(trigger);
        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);
        AccurevSCM scm = new AccurevSCM(AccurevSCM.createDepotList(sampleWorkspace.host, sampleWorkspace.port, c.getId()), Collections.singletonList(new StreamSpec(depot, depot)), Collections.emptyList());
        project.setScm(scm);
        trigger.start(project, true);
        project.setQuietPeriod(0);
        
        rule.jenkins.setCrumbIssuer(new DefaultCrumbIssuer(false));
        attachPromoteTrigger(depot);
        sampleWorkspace.commit("Test", sampleWorkspace.username, "Initial promote");



        Thread.sleep(2500);
        assertEquals(1, project.getLastBuild().number);
    }

    @Test
    public void testMQTTBrokerConnection() throws Exception {
        // Initialize workspace
        sampleWorkspace.init("localhost", "5050", "accurev_user", "docker");
        // Creates depot and stream
        String depot = sampleWorkspace.mkDepot();
        // Create a workspace so we can promote a File
        sampleWorkspace.mkWorkspace(depot);


        assertEquals( "", getStreamBuiltState(depot));

        String content = "MQTT-test-broker" + "\n"
                + "SUCCESS" + "\n";

        String transaction = "1";
        String topic = "gatedStream/" + depot + "/" + transaction;

        sendMQTTMessage(topic, content);

        String brokerLog = getBrokerLog();
        assertTrue(brokerLog.contains("Transaction number: " + transaction));
        assertTrue(brokerLog.contains(depot));

        assertEquals("success", getStreamBuiltState(depot));


        content = "MQTT-test-broker" + "\n"
                + "FAILURE" + "\n";

        sendMQTTMessage(topic, content);

        assertEquals("failed", getStreamBuiltState(depot));
    }

    @Test
    public void fullTripTest() throws Exception {
        // Initialize workspace
        sampleWorkspace.init("localhost", "5050", "accurev_user", "docker");
        // Creates depot and stream
        String depot = sampleWorkspace.mkDepot();
        // Create a workspace so we can promote a File
        sampleWorkspace.mkWorkspace(depot);
        // Create workflow job
        WorkflowJob project = rule.jenkins.createProject(WorkflowJob.class, "demo");
        // Add accurev credentials to store
        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);
        // Check that no properties are set on the stream
        assertEquals( "", getStreamBuiltState(depot));
        // Add a trigger to the job, so it's possible to trigger from webhooks
        SCMTrigger trigger = new SCMTrigger("");
        project.addTrigger(trigger);
        trigger.start(project, true);
        // Set definition, do a simple populate and respond to MQTT afterwards with the build result
        project.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "agent any\n" +
                        "   stages {\n" +
                        "       stage('single') {\n" +
                        "           steps ('checkout') {\n" +
                        "                   accurev host: '" + sampleWorkspace.host + "', port: '"+  sampleWorkspace.port + "', depot: '" + depot + "', stream: '" + depot + "', credentialsId: '" + c.getId() + "'  \n" +
                        "                   \n" +
                        "           }\n" +
                        "       }\n" +
                        "   }\n" +
                        "   post {\n" +
                        "           always {\n" +
                        "                   mqttResponse()\n" +
                        "           }\n" +
                        "   }\n" +
                        "}\n", true));
        // Build the job so the FlowDefinition is properly loaded
        rule.assertBuildStatusSuccess(project.scheduleBuild2(0));
        // Assert we only have one SCM attached to our build
        assertEquals(1, project.getSCMs().size());
        // assertEquals(type AccurevSCM)

        // Set crumb
        rule.jenkins.setCrumbIssuer(new DefaultCrumbIssuer(false));

        // Set quiet period to 0, so we build once we get triggered
        project.setQuietPeriod(0);
        // Get the port from the JenkinsRule - When JenkinsRule runs it starts Jenkins at a random port
        String response = attachPromoteTrigger(depot);

        assertEquals("Created trigger server-post-promote-trig /home/accurev-user/accurev/storage/site_slice/triggers/server_post_promote_hook", response);
        sampleWorkspace.commit("Test", sampleWorkspace.username, "Initial promote");

        // We need to give Accurev a chance to parse the newly committed file and issue a trigger
        Thread.sleep(2000);
        assertTrue(getTriggerLog().contains("server_post_promote triggered"));
        // Check we received a webhook and built the job
        assertEquals(2, project.getLastBuild().number);

        // Assert that our MQTT broker has received a response from jenkins, with our newly build transaction
        assertTrue(getBrokerLog().contains("Transaction number: 4"));

        // Assert that the stream that was built now has a property for SUCCESS
        assertEquals("success", getStreamBuiltState(depot));

    }

    private void sendMQTTMessage(String topic, String content) throws UnsupportedEncodingException {
        int qos = 2;
        // Testing purposes
        String broker = "tcp://localhost:8883";
        String clientId = "Jenkins MQTT";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println(("Connecting to broker: " + broker));
            sampleClient.connect(connOpts);
            System.out.println(("Connected"));
            System.out.println(("Publishing message: " + content));
            MqttMessage message = new MqttMessage(content.getBytes("UTF-8"));
            message.setQos(qos);
            sampleClient.publish(topic, message);
            System.out.println(("Message published"));
            sampleClient.disconnect();
            System.out.println(("Disconnected"));
        } catch (MqttException me) {
            System.out.println(("reason " + me.getReasonCode()));
            System.out.println(("msg " + me.getMessage()));
            System.out.println(("loc " + me.getLocalizedMessage()));
            System.out.println(("cause " + me.getCause()));
            System.out.println(("excep " + me));
            me.printStackTrace();
        }
    }

    private String getBrokerLog() throws IOException, InterruptedException {
        DockerComposeExecOption options = new DockerComposeExecOption() {
            @Override
            public List<String> options() {
                return Collections.emptyList();
            }
        };
        DockerComposeExecArgument arguments = new DockerComposeExecArgument() {
            @Override
            public List<String> arguments() {
                List<String> arg = new ArrayList<>();
                arg.add("cat");
                arg.add("accurev/storage/site_slice/logs/brokerLog.log");
                return arg;
            }
        };
        return docker.exec(options, "accurev", arguments);
    }

    private String attachPromoteTrigger(String d) throws IOException, InterruptedException {
        DockerComposeExecOption options = new DockerComposeExecOption() {
            @Override
            public List<String> options() {
                return Collections.emptyList();
            }
        };
        DockerComposeExecArgument arguments = new DockerComposeExecArgument() {
            @Override
            public List<String> arguments() {
                List<String> arg = new ArrayList<>();
                arg.add("accurev");
                arg.add("mktrig");
                arg.add("-p");
                arg.add(d);
                arg.add("server-post-promote-trig");
                arg.add("/home/accurev-user/accurev/storage/site_slice/triggers/server_post_promote_hook");
                return arg;
            }
        };
        return docker.exec(options, "accurev", arguments);
    }

    private String getTriggerLog() throws IOException, InterruptedException {
        DockerComposeExecOption options = new DockerComposeExecOption() {
            @Override
            public List<String> options() {
                return Collections.emptyList();
            }
        };
        DockerComposeExecArgument arguments = new DockerComposeExecArgument() {
            @Override
            public List<String> arguments() {
                List<String> arg = new ArrayList<>();
                arg.add("cat");
                arg.add("accurev/storage/site_slice/logs/trigger.log");
                return arg;
            }
        };
        return docker.exec(options, "accurev", arguments);
    }

    private String getStreamBuiltState(String stream) throws IOException, InterruptedException {
        DockerComposeExecOption options = new DockerComposeExecOption() {
            @Override
            public List<String> options() {
                return Collections.emptyList();
            }
        };
        DockerComposeExecArgument arguments = new DockerComposeExecArgument() {
            @Override
            public List<String> arguments() {
                List<String> arg = new ArrayList<>();
                arg.add("accurev");
                arg.add("getproperty");
                arg.add("-s");
                arg.add(stream);
                arg.add("-r");
                arg.add("-fx");
                return arg;
            }
        };
        String properties = docker.exec(options, "accurev", arguments);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            properties = properties.replace("\n", "").replace("\r", "");
            builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(properties));
            Document document = builder.parse(inputSource);
            Node item = document.getElementsByTagName("property").item(1);
            return item.getTextContent();
        } catch (Exception e) {
            return "";
        }

    }
}
