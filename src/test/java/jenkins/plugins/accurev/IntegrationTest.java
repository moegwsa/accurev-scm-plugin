package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.StreamSpec;
import hudson.plugins.accurev.util.AccurevTestExtensions;
import hudson.triggers.SCMTrigger;
import jenkins.branch.BranchProperty;
import jenkins.branch.BranchSource;
import jenkins.branch.DefaultBranchPropertyStrategy;
import jenkins.plugins.accurev.traits.BuildItemsDiscoveryTrait;
import jenkins.plugins.accurevclient.AccurevClient;
import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.*;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class IntegrationTest {


    @Rule public TestName name = new TestName();

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Rule
    public DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/docker/docker-compose.yml")
            .saveLogsTo("src/docker/logs-" + name.getMethodName())
            .build();

    String host = "localhost";
    String port = "5050";

    private AccurevClient client;

    private static String url;
    private static String username;
    private static String password;

    @BeforeClass
    public static void init() throws IOException, InterruptedException {
        url = System.getenv("_ACCUREV_URL") == "" ? System.getenv("_ACCUREV_URL") : "localhost:5050";
        username = System.getenv("_ACCUREV_USERNAME") != null ? System.getenv("_ACCUREV_URL") : "accurev_user";
        password = System.getenv("_ACCUREV_PASSWORD") != null ? System.getenv("_ACCUREV_URL") : "docker";
        assumeTrue("Can only run test with proper test setup",
                AccurevTestExtensions.checkCommandExist("accurev") &&
                        StringUtils.isNotBlank(url) &&
                        StringUtils.isNotBlank(username) &&
                        StringUtils.isNotEmpty(password)
        );
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        // Get the port from the JenkinsRule - When JenkinsRule runs it starts Jenkins at a random port
        String jenkinsPort = Integer.toString(rule.getURL().getPort());
        //        // For docker.exec command, no options needed.
        DockerComposeExecOption options = new DockerComposeExecOption() {
            @Override
            public List<String> options() {
                return Collections.emptyList();
            }
        };
        // Exec into the container, updating the url pointing to Jenkins with the correct port
        DockerComposeExecArgument arguments = new DockerComposeExecArgument() {
            @Override
            public List<String> arguments() {
                List<String> arg = new ArrayList<>();
                arg.add("perl");
                arg.add("./updateJenkinsHook.pl");
                arg.add(jenkinsPort);
                arg.add("http://host.docker.internal");
                return arg;
            }
        };
        String out = docker.exec(options, "accurev", arguments);

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
                arg.add("accurev/storage/site_slice/triggers/jenkinsConfig.json");
                return arg;
            }
        };
        docker.exec(options, "accurev", arguments);
        assertTrue((docker.exec(options, "accurev", arguments).contains(jenkinsPort)));

    }

    @Test
    public void testContainerConnectionToJenkins() throws Exception{
        String jenkinsUrl = rule.getURL().toString().replace("localhost","host.docker.internal");
        DockerComposeExecOption options = new DockerComposeExecOption() {
            @Override
            public List<String> options() {
                return Collections.emptyList();
            }
        };

        // Exec into the container, checking if the container can see jenkins
        DockerComposeExecArgument arguments = new DockerComposeExecArgument() {
            @Override
            public List<String> arguments() {
            List<String> arg = new ArrayList<>();
            arg.add("curl");
            arg.add("-Is");
            arg.add(jenkinsUrl);
            return arg;
            }
        };
        assertTrue(docker.exec(options, "accurev", arguments).contains("200"));
    }


    @Test
    public void testWebhookConnection() throws Exception {
        FreeStyleProject project = rule.getInstance().createProject(FreeStyleProject.class, "test");
        client = AccurevTestExtensions.createClientAtDir(project.getBuildDir(), url, username, password);
        String depot = AccurevTestExtensions.generateString(10);
        client.depot().create(depot).execute();
        String workspace = AccurevTestExtensions.generateString(10);
        client.workspace().create(workspace, depot).execute();


        SCMTrigger trigger = new SCMTrigger("");
        project.addTrigger(trigger);
        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);
        AccurevSCM scm = new AccurevSCM(AccurevSCM.createDepotList(host, port, c.getId()), Collections.singletonList(new StreamSpec(depot, depot)), Collections.emptyList(), null);
        project.setScm(scm);
        trigger.start(project, true);
        project.setQuietPeriod(0);
        
        //rule.jenkins.setCrumbIssuer(new DefaultCrumbIssuer(false));
        rule.jenkins.disableSecurity();
        //System.setProperty("hudson.security.csrf.DefaultCrumbIssuer.EXCLUDE_SESSION_ID", "true");
        System.err.println(rule.jenkins.isUseCrumbs());
        attachPromoteTrigger(depot);
        File file = AccurevTestExtensions.createFile(project.getBuildDir().getPath(), "file", "test");
        List<String> files = new ArrayList<>();
        files.add(file.getAbsolutePath());
        client.add().add(files).comment("test").execute();
        client.promote().files(files).comment("test").execute();
        Thread.sleep(20000);
        String tl = getTriggerLog();
        System.out.println(tl);
        assertEquals(1, Objects.requireNonNull(project.getLastBuild()).number);

    }

    @Test
    public void testMQTTBrokerConnection() throws Exception {
        FreeStyleProject project = rule.getInstance().createProject(FreeStyleProject.class, "test");
        client = AccurevTestExtensions.createClientAtDir(project.getBuildDir(), url, username, password);
        // Initialize workspace
        String depot = AccurevTestExtensions.generateString(10);
        client.depot().create(depot).execute();

        String workspace = AccurevTestExtensions.generateString(10);
        client.workspace().create(workspace, depot).execute();


        assertEquals( "", getStreamBuiltState(depot));

        String content = "MQTT-test-broker" + "\n"
                + "SUCCESS" + "\n";

        String transaction = "1";
        String topic = "gatedStream/" + depot + "/" + transaction;

        sendMQTTMessage(topic, content);

        String brokerLog = getBrokerLog();
        System.out.println(brokerLog);
        assertTrue(brokerLog.contains("Transaction built: " + transaction));
        assertTrue(brokerLog.contains(depot));

        assertEquals("success", getStreamBuiltState(depot));


        content = "MQTT-test-broker" + "\n"
                + "FAILURE" + "\n";

        sendMQTTMessage(topic, content);

        assertEquals("failed", getStreamBuiltState(depot));
    }

    @Test(timeout = 300000)
    public void fullTripTest() throws Exception {
        WorkflowJob project = rule.jenkins.createProject(WorkflowJob.class, "demo");
        client = AccurevTestExtensions.createClientAtDir(project.getBuildDir(), url, username, password);
        String depot = AccurevTestExtensions.generateString(10);
        client.depot().create(depot).execute();
        String workspace = AccurevTestExtensions.generateString(10);
        client.workspace().create(workspace, depot).execute();

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
                        "                   accurev host: '" + host + "', port: '"+  port + "', depot: '" + depot + "', stream: '" + depot + "', credentialsId: '" + c.getId() + "'  \n" +
                        "                   \n" +
                        "           }\n" +
                        "       }\n" +
                        "   }\n" +
                        "   post {\n" +
                        "           always {\n" +
                        "                   mqttResponse('localhost:8883')\n" +
                        "           }\n" +
                        "   }\n" +
                        "}\n", true));
        // Build the job so the FlowDefinition is properly loaded
        rule.assertBuildStatusSuccess(project.scheduleBuild2(0));
        // Assert we only have one SCM attached to our build
        assertEquals(1, project.getSCMs().size());
        // assertEquals(type AccurevSCM)

        // Set crumb
        //rule.jenkins.setCrumbIssuer(new DefaultCrumbIssuer(false));
        rule.jenkins.disableSecurity();
        rule.jenkins.save();
        System.err.println(rule.jenkins.isUseCrumbs());
        // Set quiet period to 0, so we build once we get triggered
        project.setQuietPeriod(0);
        // Get the port from the JenkinsRule - When JenkinsRule runs it starts Jenkins at a random port
        String response = attachPromoteTrigger(depot);

        assertEquals("Created trigger server-post-promote-trig /home/accurev-user/accurev/storage/site_slice/triggers/server_post_promote_hook", response);

        File file = AccurevTestExtensions.createFile(project.getBuildDir().getPath(), "file", "test");
        List<String> files = new ArrayList<>();
        files.add(file.getAbsolutePath());
        client.add().add(files).comment("test").execute();
        client.promote().files(files).comment("test").execute();

        // We need to give Accurev a chance to parse the newly committed file and issue a trigger
        Thread.sleep(20000);
        String tl = getTriggerLog();
        System.out.println(tl);
        assertTrue(tl.contains("server_post_promote triggered"));
        // Check we received a webhook and built the job
        assertEquals(2, project.getLastBuild().number);
        String bl = getBrokerLog();
        System.out.println(bl);
        // Assert that our MQTT broker has received a response from jenkins, with our newly build transaction
        assertTrue(bl.contains("Transaction built: 4"));
        // Assert that the stream that was built now has a property for SUCCESS
        assertEquals("success", getStreamBuiltState(depot));

    }

    @Test
    public void HideEmptyStatingStreamsProjectTest() throws Exception{
        rule.jenkins.disableSecurity();
        rule.jenkins.save();

        WorkflowMultiBranchProject multiProject = rule.jenkins.createProject(WorkflowMultiBranchProject.class, "demo");

        client = AccurevTestExtensions.createClientAtDir(multiProject.getComputationDir(), url, username, password);
        String depot = AccurevTestExtensions.generateString(10);

        // Add accurev credentials to store
        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);


        File jenkinsFile = AccurevTestExtensions.createFile(
                multiProject.getComputationDir().getPath(),
                "Jenkinsfile",
                "pipeline {\n" +
                        "agent any\n" +
                        "   stages {\n" +
                        "       stage('single') {\n" +
                        "           steps ('checkout') {\n" +
                        "                   accurev host: '" + host + "', port: '"+  port + "', depot: '" + depot + "', stream: '" + depot + "', credentialsId: '" + c.getId() + "'  \n" +
                        "                   \n" +
                        "           }\n" +
                        "       }\n" +
                        "   }\n" +
                        "   post {\n" +
                        "           always {\n" +
                        "                   mqttResponse('localhost:8883')\n" +
                        "           }\n" +
                        "   }\n" +
                        "}\n");
                // Create Depot with trigger
        client.depot().create(depot).execute();
        attachPromoteTrigger(depot);

        //Add jenkinsfile to top stream
        String workspace1 = AccurevTestExtensions.generateString(10);
        client.workspace().create(workspace1, depot).execute();
        List<String> files = new ArrayList<>();
        files.add(jenkinsFile.getAbsolutePath());
        client.add().add(files).comment("test").execute();
        client.promote().files(files).comment("test").execute();

        //Create a gated stream
        String stream = AccurevTestExtensions.generateString(10);
        client.stream().create(stream, depot,true).execute();

        // Discover Staging streams
        AccurevSCMSource accurevSCMSource = new AccurevSCMSource(null, "localhost", "5050", depot, "1");
        accurevSCMSource.setTraits(Collections.singletonList(new BuildItemsDiscoveryTrait(true, false,false,false,true, false)));

        // Find builds
        multiProject.getSourcesList().add(new BranchSource(accurevSCMSource, new DefaultBranchPropertyStrategy(new BranchProperty[0])));
        multiProject.scheduleBuild2(0).getFuture().get();

        rule.waitUntilNoActivity();

        // should be transaction 4.
        assertTrue(getBrokerLog().contains("Transaction built: 4"));

        // We only expects to find the top stream due to not listing gated streams.
        assertEquals(1, multiProject.getAllJobs().size());

        //Create workspace under gated stream
        client = AccurevTestExtensions.createClientAtDir(multiProject.getJobsDir(), url, username, password);
        String workspace2 = AccurevTestExtensions.generateString(10);
        client.workspace().create(workspace2, stream).execute();

        // Promote new file, should trigger an update
        File file = AccurevTestExtensions.createFile(multiProject.getJobsDir().getPath(), "File",  "initial file");
        files = new ArrayList<>();
        files.add(file.getAbsolutePath());
        client.add().add(files).comment("test new file").execute();
        client.promote().files(files).comment("test new file").execute();
        Thread.sleep(20000);
        rule.waitUntilNoActivity();

        // We should now have both the top stream, and the staging stream.
        assertEquals(2, multiProject.getAllJobs().size());
        Thread.sleep(20000);

        // should be transaction number 9.
        assertTrue(getBrokerLog().contains("Transaction built: 9"));

        // search for streams again due to not sending a delete event from broker
        multiProject.scheduleBuild2(0).getFuture().get();
        rule.waitUntilNoActivity();

        // Now only the top stream should exists
        long size = multiProject.getAllJobs().size();
        assertEquals(1, multiProject.getAllJobs().size());
    }

    @Test
    public void TransactionNumberAutoPromote() throws Exception{
        rule.jenkins.disableSecurity();
        rule.jenkins.save();

        WorkflowMultiBranchProject multiProject = rule.jenkins.createProject(WorkflowMultiBranchProject.class, "demo");
        client = AccurevTestExtensions.createClientAtDir(multiProject.getComputationDir(), url, username, password);
        String depot = AccurevTestExtensions.generateString(10);

        // Add accurev credentials to store
        IdCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "1", null, "accurev_user", "docker");
        CredentialsProvider.lookupStores(rule.jenkins).iterator().next()
                .addCredentials(Domain.global(), c);

        File jenkinsFile = AccurevTestExtensions.createFile(
                multiProject.getComputationDir().getPath(),
                "Jenkinsfile",
                "pipeline {\n" +
                        "agent any\n" +
                        "   stages {\n" +
                        "       stage('single') {\n" +
                        "           steps ('checkout') {\n" +
                        "                   accurev host: '" + host + "', port: '"+  port + "', depot: '" + depot + "', stream: '" + depot + "', credentialsId: '" + c.getId() + "'  \n" +
                        "                   \n" +
                        "           }\n" +
                        "       }\n" +
                        "   }\n" +
                        "   post {\n" +
                        "           always {\n" +
                        "                   mqttResponse('localhost:8883')\n" +
                        "           }\n" +
                        "   }\n" +
                        "}\n");
        // Create Depot with trigger
        client.depot().create(depot).execute(); //Transaction 1

        //Add jenkinsfile to stream
        String workspace1 = AccurevTestExtensions.generateString(10);
        client.workspace().create(workspace1, depot).execute();
        List<String> files = new ArrayList<>();
        files.add(jenkinsFile.getAbsolutePath());
        client.add().add(files).comment("test").execute(); //Transaction 3
        client.promote().files(files).comment("test").execute(); //Transaction 4

        //Attach trigger
        attachPromoteTrigger(depot);

        // Discover streams
        AccurevSCMSource accurevSCMSource = new AccurevSCMSource(null, "localhost", "5050", depot, "1");
        accurevSCMSource.setTraits(Collections.singletonList(new BuildItemsDiscoveryTrait(true, false,false,false,true, false)));

        // Find builds
        multiProject.getSourcesList().add(new BranchSource(accurevSCMSource, new DefaultBranchPropertyStrategy(new BranchProperty[0])));
        multiProject.scheduleBuild2(0).getFuture().get();

        rule.waitUntilNoActivity();
        Thread.sleep(20000);



        // Only expect multibranch scan to find one job
        assertEquals(1, multiProject.getAllJobs().iterator().next().getBuilds().size());

        assertTrue(getBrokerLog().contains("Transaction built: 4"));

        // Promote new file, should trigger an update
        File file = AccurevTestExtensions.createFile(multiProject.getComputationDir().getPath(), "File",  "initial file");
        files = new ArrayList<>();
        files.add(file.getAbsolutePath());
        client.add().add(files).comment("test new file").execute(); //Transaction 5
        client.promote().files(files).comment("test new file").execute(); //Transaction 6

        Thread.sleep(20000);
        rule.waitUntilNoActivity();
        assertTrue(getBrokerLog().contains("Transaction built: 6"));


        // Triggered build should be build two
        assertEquals(2, multiProject.getAllJobs().iterator().next().getBuilds().size());

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

    private String attachPromoteTrigger(String depot) throws IOException, InterruptedException {
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
                arg.add(depot);
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
                arg.add("/home/accurev-user/accurev/storage/site_slice/logs/trigger.log");
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
