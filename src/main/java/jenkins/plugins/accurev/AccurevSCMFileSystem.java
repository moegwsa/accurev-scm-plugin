package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.ServerRemoteConfig;
import hudson.plugins.accurev.StreamSpec;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.security.ACL;
import jenkins.plugins.accurevclient.Accurev;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;

import java.io.IOException;


public class AccurevSCMFileSystem extends SCMFileSystem {

    public String getHead() {
        return head;
    }

    private final String head;
    private final String remote;
//    private final Long transactionId;

    public AccurevClient getAccurevClient() {
        return accurevClient;
    }

    private final AccurevClient accurevClient;

    public AccurevSCMFileSystem(AccurevClient accurevClient, String remote, String head, AccurevSCMSource.SCMRevisionImpl rev) {
        super(rev);
        this.remote = remote;
        this.head = head;
        this.accurevClient = accurevClient;
    }

    @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @Override
    public long lastModified() throws IOException, InterruptedException {
        if(accurevClient.getCredentials() != null) {
            StandardUsernamePasswordCredentials credentials = accurevClient.getCredentials();
            if(credentials != null) {
                accurevClient.login().username(credentials.getUsername()).password(credentials.getPassword()).execute();
                long id = accurevClient.fetchTransaction(head).getId();
                return id;
            }
        }
        return getRoot().lastModified();
    }

    @Override
    public AccurevSCMSource.SCMRevisionImpl getRevision() {
        return (AccurevSCMSource.SCMRevisionImpl) super.getRevision();
    }

    @NonNull
    @Override
    public SCMFile getRoot() {
        return new AccurevSCMFile(this);
    }

    public String getRemote() {
        return remote;
    }

    @Extension(ordinal = Short.MIN_VALUE)
    public static class BuilderImpl extends SCMFileSystem.Builder {
        @Override
        public boolean supports(SCM source) {
            return source instanceof AccurevSCM;
        }

        @Override
        public boolean supports(SCMSource source) {
            return source instanceof AccurevSCMSource;
        }

        @Override
        protected boolean supportsDescriptor(SCMDescriptor descriptor) {
            return false;
        }

        @Override
        protected boolean supportsDescriptor(SCMSourceDescriptor descriptor) {
            return false;
        }

        @Override
        public SCMFileSystem build(@NonNull SCMSource source, @NonNull SCMHead head, SCMRevision rev)
                throws IOException, InterruptedException {
           if ( rev != null && !(rev instanceof AccurevSCMSource.SCMRevisionImpl)) {
               return null;
           }
           AccurevSCMSource accurevSCMSource = (AccurevSCMSource) source;
           Accurev accurev = new Accurev();
           accurev.setUrl(accurevSCMSource.getRemote());
           AccurevClient accurevClient = accurev.getClient();
           accurevClient.setCredentials(accurevSCMSource.getCredentials());
           return new AccurevSCMFileSystem(accurevClient, accurevSCMSource.getRemote(), head.getName(), (AccurevSCMSource.SCMRevisionImpl) rev);
        }

        @Override
        public SCMFileSystem build(@NonNull Item owner, @NonNull SCM scm, SCMRevision rev) throws IOException, InterruptedException {
            if (rev != null && !(rev instanceof AccurevSCMSource.SCMRevisionImpl)) return null;

            AccurevSCM accurevSCM = (AccurevSCM) scm;
            ServerRemoteConfig config = accurevSCM.getServerRemoteConfigs().get(0);
            StreamSpec streamSpec = accurevSCM.getStreams().get(0);
            String remote = config.getUrl();
            Accurev accurev = new Accurev();
            AccurevClient accurevClient = accurev.on(remote).getClient();
            String credentialsId = config.getCredentialsId();
            if (credentialsId != null) {
                StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(
                                StandardUsernamePasswordCredentials.class,
                                owner,
                                ACL.SYSTEM,
                                URIRequirementBuilder.fromUri(remote).build()
                        ),
                        CredentialsMatchers.allOf(
                                CredentialsMatchers.withId(credentialsId),
                                AccurevClient.Companion.getCREDENTIALS_MATCHER()
                        )
                );
                accurevClient.setCredentials(credentials);
            }
            String headName;
            if (rev != null) {
                headName = rev.getHead().getName();
            } else {
                headName = streamSpec.getName();
            }

            return new AccurevSCMFileSystem(accurevClient, remote, headName, (AccurevSCMSource.SCMRevisionImpl) rev);
        }
    }
}
