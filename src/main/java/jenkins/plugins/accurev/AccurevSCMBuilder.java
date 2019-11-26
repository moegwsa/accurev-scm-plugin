package jenkins.plugins.accurev;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.ServerRemoteConfig;
import hudson.plugins.accurev.StreamSpec;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.trait.SCMBuilder;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AccurevSCMBuilder<B extends AccurevSCMBuilder<B>> extends SCMBuilder<B, AccurevSCM> {

    private final String remote;
    private String credentialsId;
    private final String depot;
    private List<AccurevSCMExtension> extensions = new ArrayList<>();


    public AccurevSCMBuilder(@NonNull SCMHead head, @CheckForNull SCMRevision revision, @NonNull String remote, String depot,
                             @CheckForNull String credentialsId) {
        super(AccurevSCM.class, head, revision);
        this.remote = remote;
        this.credentialsId = credentialsId;
        this.depot  = depot;
    }

    @NonNull
    @Override
    public AccurevSCM build() {
        return new AccurevSCM(asRemoteConfigs(),
                Collections.singletonList(new StreamSpec(head().getName(), depot)),
                extensions
                );
    }

    private final List<ServerRemoteConfig> asRemoteConfigs() {
        List<ServerRemoteConfig> result = new ArrayList<>();
        String[] remoteParts = remote().split(":");
        result.add(new ServerRemoteConfig(remoteParts[0], remoteParts[1], credentialsId()));
        return result;
    }

    public String credentialsId() {
        return credentialsId;
    }

    private String remote() {
        return remote;
    }

    @NonNull
    public final B withExtensions(AccurevSCMExtension... extensions) {
        return withExtensions(Arrays.asList(extensions));
    }

    @NonNull
    public final B withExtensions(@NonNull List<AccurevSCMExtension> extensions) {
        for (AccurevSCMExtension extension : extensions) {
            withExtension(extension);
        }
        return (B) this;
    }

    @NonNull
    public final B withExtension(@edu.umd.cs.findbugs.annotations.CheckForNull AccurevSCMExtension extension) {
        if (extension != null) {
            // the extensions only allow one of each type.
            for (Iterator<AccurevSCMExtension> iterator = extensions.iterator(); iterator.hasNext(); ) {
                if (extension.getClass().equals(iterator.next().getClass())) {
                    iterator.remove();
                }
            }
            extensions.add(extension);
        }
        return (B) this;
    }

    public final B withCredentials(@CheckForNull String credentialsId) {
        this.credentialsId = credentialsId;
        return (B) this;
    }

    @NonNull
    public final List<AccurevSCMExtension> extensions() {
        return Collections.unmodifiableList(extensions);
    }
}
