package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.ServerRemoteConfig;
import hudson.plugins.accurev.StreamSpec;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.security.ACL;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.api.SCMSourceOwner;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public abstract class AccurevSCMFileSystemBuilder extends SCMFileSystem.Builder {

    @Override
    public boolean supports(SCM scm) {
        if (scm instanceof AccurevSCM) {
            return true;
        }
        return false;
    }

    @Override
    public boolean supports(SCMSource scmSource) {
        return scmSource instanceof AccurevSCMSource;
    }

    public abstract boolean supports(@NonNull String remote);

    @Override
    protected boolean supportsDescriptor(SCMDescriptor scmDescriptor) {
        return false;
    }

    @Override
    protected boolean supportsDescriptor(SCMSourceDescriptor scmSourceDescriptor) {
        return false;
    }

    @Override
    public SCMFileSystem build(@NonNull Item item, @NonNull SCM scm, SCMRevision scmRevision) throws IOException, InterruptedException {
        if (scm instanceof AccurevSCM) {
            AccurevSCM accurevSCM = (AccurevSCM) scm;
            List<StreamSpec> streams = accurevSCM.getStreams();
            List<ServerRemoteConfig> configs = accurevSCM.getServerRemoteConfigs();

            if(configs.size() == 1 && streams.size() == 1) {
                ServerRemoteConfig config = configs.get(0);
                String credentialsId = config.getCredentialsId();
                StandardCredentials credentials;
                String remote = config.getUri().toString();
                if (credentialsId != null) {
                    List<StandardUsernameCredentials> urlCredentials = CredentialsProvider
                            .lookupCredentials(StandardUsernameCredentials.class, item,
                                    item instanceof Queue.Task
                                            ? Tasks.getAuthenticationOf((Queue.Task) item)
                                            : ACL.SYSTEM, URIRequirementBuilder.fromUri(remote).build());

                    credentials = CredentialsMatchers.firstOrNull(
                            urlCredentials,
                            CredentialsMatchers
                                    .allOf(CredentialsMatchers.withId(credentialsId), AccurevClient.Companion.getCREDENTIALS_MATCHER())
                    );
                } else {
                    credentials = null;
                }
                SCMHead head = null;
                if (!(scmRevision == null)) {
                    head = scmRevision.getHead();
                }
                return build(remote, credentials, head, scmRevision);

            }

        }
        return null;
    }

    private SCMFileSystem build(String remote, StandardCredentials credentials, @Nullable SCMHead head, SCMRevision scmRevision) {
        AccurevClient accurevClient = null;
        if (scmRevision != null) {
            AccurevSCMSource.SCMRevisionImpl myRev = new AccurevSCMSource.SCMRevisionImpl(head, Long.parseLong(scmRevision.toString()));
            return new AccurevSCMFileSystem(accurevClient, remote, head.toString(), myRev);
        }
        return null;
    }

    public abstract SCMRevision getRevision(@NonNull String remote,
                                            @edu.umd.cs.findbugs.annotations.CheckForNull StandardCredentials credentials,
                                            @NonNull String refOrHash)
            throws IOException, InterruptedException;


    public final SCMFileSystem build(@NonNull SCMSource source, @NonNull SCMHead head, @CheckForNull SCMRevision rev) {
        SCMSourceOwner owner = source.getOwner();
        if (source instanceof AccurevSCMSource && owner != null && supports(
                ((AccurevSCMSource) source).getRemote())) {
            AccurevSCMSource accurev = (AccurevSCMSource) source;
            String remote = accurev.getRemote();
            StandardUsernameCredentials credentials = accurev.getCredentials();
            return build(remote, credentials, head, rev);
        }
        return null;
    }

    public abstract long getTimestamp(String remote, StandardCredentials credentials, String stream);
}
