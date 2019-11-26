package hudson.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Objects;

@ExportedBean
public class ServerRemoteConfig extends AbstractDescribableImpl<ServerRemoteConfig> implements Serializable {


    private String host;
    private String port;
    private String credentialsId;
    private URIish uri;

    @DataBoundConstructor
    public ServerRemoteConfig(String host, String port, String credentialsId){
        this.host = host;
        this.port = port;
        this.credentialsId = credentialsId;
        try {
            this.uri = new URIish(host + ":" + port);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Exported
    public String getHost() {
        return host;
    }

    @Exported
    public String getPort() {
        return port;
    }

    @Exported
    public String getCredentialsId() {
        return credentialsId;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Exported
    public URIish getUri() {
        if(uri != null) return uri;

        try {
            return new URIish(host + ":" + port);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerRemoteConfig that = (ServerRemoteConfig) o;
        return host.equals(that.host) &&
                port.equals(that.port) &&
                credentialsId.equals(that.credentialsId) &&
                uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, credentialsId, uri);
    }

    @Exported
    public String getUrl() {
        return host + ":" + port;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ServerRemoteConfig> {

        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item item,
                @QueryParameter String credentialsId
        ) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(ACL.SYSTEM,
                            Jenkins.getInstance(),
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.fromUri("").build(),
                            CredentialsMatchers.always()
                    )
                    .includeCurrentValue(credentialsId);
        }

        @Override
        public String getDisplayName() {
            return "Server Remote Config";
        }
    }
}
