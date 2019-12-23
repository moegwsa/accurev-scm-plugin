package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.plugins.accurev.AccurevSCMRevision;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.plugins.accurev.traits.BuildItemsDiscoveryTrait;
import jenkins.plugins.accurevclient.Accurev;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.model.AccurevStream;
import jenkins.plugins.accurevclient.model.AccurevStreamType;
import jenkins.plugins.accurevclient.model.AccurevStreams;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMProbe;
import jenkins.scm.api.SCMProbeStat;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.form.NamedArrayList;
import jenkins.scm.impl.trait.Discovery;
import jenkins.scm.impl.trait.Selection;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.verb.POST;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class AccurevSCMSource extends SCMSource {

    private final String remote;
    private String sourceHost;
    private String sourcePort;
    private final String depot;
    private AccurevClient accurevClient;
    private String credentialsId;

    @NonNull
    private List<SCMSourceTrait> traits = new ArrayList<>();

    public static final Logger LOGGER = Logger.getLogger(AccurevSCMSource.class.getName());

    @Override
    @NonNull
    public List<SCMSourceTrait> getTraits() {
        return Collections.unmodifiableList(traits);
    }

    public String getRemote() {
        return remote;
    }

    public String getDepot() {
        return depot;
    }

    public String getSourceHost() {
        return sourceHost;
    }

    public String getSourcePort() {
        return sourcePort;
    }

    public AccurevClient getAccurevClient() {
        return accurevClient;
    }

    @DataBoundSetter
    public void setSourcePort(String sourcePort) {
        this.sourcePort = sourcePort;
    }

    @DataBoundSetter
    public void setSourceHost(String sourceHost) {
        this.sourceHost = sourceHost;
    }

    @DataBoundSetter
    public void setTraits(@CheckForNull List<SCMSourceTrait> traits) {
        this.traits = new ArrayList<>(Util.fixNull(traits));
    }

    @Override
    protected void retrieve(SCMSourceCriteria scmSourceCriteria, @NonNull SCMHeadObserver scmHeadObserver, SCMHeadEvent<?> scmHeadEvent, @NonNull TaskListener taskListener) throws IOException, InterruptedException {
        taskListener.getLogger().println("Retrieving from Accurev");
        this.listener = taskListener;

        AccurevSCMSourceContext context = new AccurevSCMSourceContext<>(scmSourceCriteria, scmHeadObserver).withTraits(getTraits());
        try (AccurevSCMSourceRequest request = context.newRequest(this, taskListener)) {

            Node instance = Jenkins.getInstance();
            Launcher launcher;
            if(instance != null) {
                launcher = instance.createLauncher(taskListener);
            }else {
                launcher = new Launcher.LocalLauncher(taskListener);
            }
            Accurev accurev = Accurev.with(taskListener, new EnvVars(), launcher).at(Jenkins.getInstanceOrNull().root).on(remote);
            accurevClient = accurev.getClient();
            accurevClient.login().username(getCredentials().getUsername()).password(getCredentials().getPassword()).execute();

            AccurevStreams streams;

            if (context.getTopStream().isEmpty()) {
                streams = accurevClient.getStreams(depot);
            } else {
                streams = accurevClient.getChildStreams(depot, context.getTopStream());
            }

            for (AccurevStream stream : streams.getList()) {

                taskListener.getLogger().println("Processing object: " + stream.getName());
                if (!context.isWantStreams() && stream.getType().equals(AccurevStreamType.Normal)) {
                    taskListener.getLogger().println("Discarded object: " + stream.getName() + ". Reason: Don't want to build normal types");
                    continue;
                }
                if (!context.isWantWorkspaces() && stream.getType().equals(AccurevStreamType.Workspace)) {
                    taskListener.getLogger().println("Discarded object: " + stream.getName() + ". Reason: Don't want to build workspaces");
                    continue;
                }
                if (!context.isWantSnapshots() && stream.getType().equals(AccurevStreamType.Snapshot)) {
                    taskListener.getLogger().println("Discarded object: " + stream.getName() + ". Reason: Don't want to build snapshots");
                    continue;
                }
                if (!context.isWantPassThroughs() && stream.getType().equals(AccurevStreamType.PassThrough)) {
                    taskListener.getLogger().println("Discarded object: " + stream.getName() + ". Reason: Don't want to build passthrough types");
                    continue;
                }
                if (!context.iswantGatedStreams() && stream.getType().equals(AccurevStreamType.Staging)) {
                    taskListener.getLogger().println("Discarded object: " + stream.getName() + ". Reason: Don't want to build gated streams");
                    continue;
                }

                AccurevTransaction highest = accurevClient.fetchTransaction(stream.getName());
                SCMHead head = new SCMHead(stream.getName());

                SCMRevisionImpl revision = new SCMRevisionImpl(head, highest.getId());
                AccurevSCMHead accurevHead = new AccurevSCMHead(revision.getHead().getName());

                if (request.process(
                        accurevHead,
                        (SCMSourceRequest.RevisionLambda) (AccurevSCMHead) -> new AccurevSCMRevision(accurevHead, revision.getHash()),
                        (aHead, aRevision) -> new StreamSCMProbe(head.getName(), revision.getHash(), accurevClient),
                        (SCMSourceRequest.Witness) (head1, revision1, isMatch) -> {
                            if (isMatch) {
                                taskListener.getLogger().println("    Met criteria");
                            } else {
                                taskListener.getLogger().println("    Does not meet criteria");
                            }
                        })
                ) ;
            }
        }
    }


    @NonNull
    @Override
    public SCM build(@NonNull SCMHead head, SCMRevision revision) {
        AccurevSCMBuilder<?> builder = newBuilder(head, revision);
        builder.withTraits(getTraits());
        return builder.build();
    }

    @NonNull
    @Override
    protected SCMProbe createProbe(@NonNull final SCMHead head, @CheckForNull final SCMRevision revision)
            throws IOException {
        /* see note on SCMProbe */

        // assuming we have a suitable implementation of SCMFileSystem
        return newProbe(head, revision);
    }


    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }


    @DataBoundConstructor
    @SuppressWarnings("unused") // by stapler
    public AccurevSCMSource(String id, String sourceHost, String sourcePort, String depot, String credentialsId) {
        super(id);
        this.sourceHost = sourceHost;
        this.sourcePort = sourcePort;
        this.remote = sourceHost + ":" + sourcePort;
        this.credentialsId = credentialsId;
        this.depot = depot;
    }

    public AccurevSCMSource(String id, String remote, String credentialsId) {
        super(id);
        this.remote = remote;
        String parts[] = remote.split(":");
        this.sourceHost = parts[0];
        this.sourcePort = parts[1];
        this.credentialsId = credentialsId;
        this.depot = "";
    }

    protected StandardUsernamePasswordCredentials getCredentials() {
        String credentialsId = getCredentialsId();
        if (credentialsId == null) {
            return null;
        }
        return CredentialsMatchers
                .firstOrNull(
                        CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, getOwner(),
                                ACL.SYSTEM, URIRequirementBuilder.fromUri(getRemote()).build()),
                        CredentialsMatchers.allOf(CredentialsMatchers.withId(credentialsId),
                                AccurevClient.Companion.getCREDENTIALS_MATCHER()));
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    protected AccurevSCMBuilder<?> newBuilder(SCMHead head, SCMRevision rev) {
        return new AccurevSCMBuilder<>(head, rev, getRemote(), depot, getCredentialsId());
    }

    @Override
    public void afterSave() {
        super.afterSave();
    }

    private transient TaskListener listener;

    public TaskListener getListener() {
        return listener;
    }

    @Symbol("accurev")
    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Accurev";
        }

        public List<SCMSourceTraitDescriptor> getTraitDescriptors() {
            return SCMSourceTrait._for(this, AccurevSCMSourceContext.class, AccurevSCMBuilder.class);
        }

        public List<SCMSourceTrait> getTraitsDefaults() {
            return Collections.<SCMSourceTrait>singletonList(new BuildItemsDiscoveryTrait(true, false, false, false, false));
        }


        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context,
                                                     @QueryParameter String remote,
                                                     @QueryParameter String credentialsId) {
            if (context == null && !Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER) ||
                    context != null && !context.hasPermission(Item.EXTENDED_READ)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            context instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) context) : ACL.SYSTEM,
                            context,
                            StandardUsernameCredentials.class,
                            URIRequirementBuilder.fromUri(remote).build(),
                            AccurevClient.Companion.getCREDENTIALS_MATCHER())
                    .includeCurrentValue(credentialsId);
        }

        public List<NamedArrayList<? extends SCMSourceTraitDescriptor>> getTraitsDescriptorLists() {
            List<NamedArrayList<? extends SCMSourceTraitDescriptor>> result = new ArrayList<>();
            List<SCMSourceTraitDescriptor> descriptors =
                    SCMSourceTrait._for(this, AccurevSCMSourceContext.class, AccurevSCMBuilder.class);
            NamedArrayList.select(descriptors, "Within Repository",
                    NamedArrayList.anyOf(
                            NamedArrayList.withAnnotation(Selection.class),
                            NamedArrayList.withAnnotation(Discovery.class)
                    ),
                    true, result);
            NamedArrayList.select(descriptors, "Additional", null, true, result);
            return result;
        }

        @POST
        public FormValidation doTestConnection(@QueryParameter("sourcePort") String sourcePort,
                                               @QueryParameter("sourceHost") String sourceHost,
                                               @QueryParameter("credentialsId") String credentialsId,
                                               @AncestorInPath Project project) {
            if (project == null) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            } else {
                project.checkPermission(Item.CONFIGURE);
            }

            if (sourceHost == null || sourcePort == null) {
                return FormValidation.error("Host and Port needs to be set");
            }
            AccurevClient client = getAccurevClient((sourceHost + ":" + sourcePort));
            List<StandardUsernamePasswordCredentials> serverCredentials = CredentialsProvider.lookupCredentials(
                    StandardUsernamePasswordCredentials.class
            );
            CredentialsMatcher srcMatcher = CredentialsMatchers.withId(credentialsId);
            CredentialsMatcher idMatcher = CredentialsMatchers.allOf(srcMatcher, AccurevClient.Companion.getCREDENTIALS_MATCHER());
            StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(serverCredentials, idMatcher);

            if (credentials != null) {
                try {
                    client.login().username(credentials.getUsername()).password(credentials.getPassword()).execute();
                } catch (Exception e) {
                    FormValidation.error(e.getMessage());
                }
            }
            return !client.getInfo().getPrincipal().equals("(not logged in") ? FormValidation.ok("Success") : FormValidation.error("Could not log in");

        }

        AccurevClient getAccurevClient(String url) {
            AccurevClient client = Accurev.with((TaskListener) () -> null, new EnvVars(), new Launcher.LocalLauncher(null)).on(url).getClient();
            return client;
        }
    }

    public static class SCMRevisionImpl extends SCMRevision {

        /**
         * The subversion revision.
         */
        private Long hash;

        public SCMRevisionImpl(SCMHead head, Long hash) {
            super(head);
            this.hash = hash;
        }

        @Exported
        public Long getHash() {
            return hash;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SCMRevisionImpl that = (SCMRevisionImpl) o;
            return StringUtils.equals(String.valueOf(hash), String.valueOf(that.hash)) && getHead().equals(that.getHead());

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return hash != null ? hash.hashCode() : 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.valueOf(hash);
        }


    }

    private class StreamSCMProbe extends SCMProbe {

        private final Long transactionId;
        private final String name;
        private final AccurevClient accurevClient;

        public StreamSCMProbe(String name, Long hash, AccurevClient accurevClient) {
            this.name = name;
            this.transactionId = hash;
            this.accurevClient = accurevClient;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long lastModified() {
            return transactionId;
        }

        @NonNull
        @Override
        public SCMProbeStat stat(@NonNull String path) throws IOException {
            try {
                accurevClient.login().username(getCredentials().getUsername()).password(getCredentials().getPassword()).execute();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String file = accurevClient.getFile(name, path, Long.toString(transactionId));
            if (!file.isEmpty()) return SCMProbeStat.fromType(SCMFile.Type.REGULAR_FILE);
            return SCMProbeStat.fromType(SCMFile.Type.NONEXISTENT);

        }

        @Override
        public void close() {

        }
    }
}
