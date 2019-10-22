package jenkins.plugins.accurev;

import com.gargoylesoftware.htmlunit.WebResponse;
import com.palantir.docker.compose.DockerComposeRule;
import hudson.plugins.accurev.util.AccurevTestExtensions;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.scm.impl.mock.AbstractSampleDVCSRepoRule;
import org.junit.ClassRule;
import org.jvnet.hudson.test.JenkinsRule;

public final class AccurevSampleWorkspaceRule extends AbstractSampleDVCSRepoRule {


    private final AccurevClient client;
    public String host;
    public String port;
    public String username;

    @Override
    public void init() throws Exception {
    }

    public AccurevSampleWorkspaceRule(AccurevClient client) {
        this.client = client;
    }

    public void accurev(String ... cmds) throws Exception {
    }

    public void init(String host, String port, String username, String password) throws Exception {
        this.host = host;
        this.port = port;
        this.username = username;
        accurev("login", username, password, "-H", (host + ":" + port));
        //run(true,tmp.getRoot(), "accurev", "info");
    }

    public String mkDepot() throws Exception {
        String depot = AccurevTestExtensions.generateString(10);
        accurev("mkdepot", "-p " + depot);
        return depot;
    }

    public String mkWorkspace(String depot) throws Exception {
        String workspace = AccurevTestExtensions.generateString(10);
        accurev("mkws", "-w", workspace, "-b", depot, "-l", ".");
        return workspace;
    }

    public String mkStream(String backingStream) throws Exception {
        String stream = AccurevTestExtensions.generateString(10);
        accurev("mkstream", "-s", stream, "-b", backingStream);
        return stream;
    }



    public void commit(String fileName, String comitter, String message) throws Exception {
        write(fileName, fileName);
        accurev("add", "-c", message, fileName);
        accurev("promote", "-c", message, fileName);
    }

    public void notifyCommit(JenkinsRule rule, String depot) throws Exception{
        synchronousPolling(rule);
        WebResponse webResponse = rule.createWebClient().goTo("accurev/notifyCommit?host=" + host + "&port=" + port + "&depot=" + depot + "&streams=" + depot + "&transaction=1", "text/plain").getWebResponse();
        // Since it takes some time to parse the request and add it to the queue, we wait for 1 sec.
        Thread.sleep(10000);
        rule.waitUntilNoActivity();
    }

    public void init(String host, String port, String username) {
        this.host = host;
        this.port = port;
    }
}
