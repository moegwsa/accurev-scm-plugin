package hudson.plugins.accurev;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Job;
import hudson.scm.SCM;
import jenkins.plugins.accurev.AccurevSCMHead;
import jenkins.plugins.accurev.AccurevSCMSource;
import jenkins.plugins.accurev.AccurevSCMSourceContext;
import jenkins.plugins.accurev.traits.TopStreamDiscoveryTrait;
import jenkins.scm.api.*;
import jenkins.scm.api.trait.SCMSourceTrait;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AccurevSCMHeadEvent<T> extends SCMHeadEvent<AccurevCommitPayload> {
    private final AccurevCommitPayload payload;
    private final Type type;


    public AccurevSCMHeadEvent(Type type, AccurevCommitPayload payload, String origin) {
        super(type, payload, origin);
        this.payload = payload;
        this.type = type;
    }

    @NonNull
    @Override
    public AccurevCommitPayload getPayload() {
        return payload;
    }

    @Override
    public boolean isMatch(@NonNull SCMNavigator scmNavigator) {
        return false;
    }



    @NonNull
    @Override
    public String getSourceName() {
        return payload.getStream();
    }

    @NonNull
    @Override
    public Map<SCMHead, SCMRevision> heads(@NonNull SCMSource scmSource) {
        if (scmSource instanceof AccurevSCMSource) {
            AccurevSCMSource accurevSCMSource = (AccurevSCMSource) scmSource;
            String remote = accurevSCMSource.getRemote();
            if (remote != null) {
                try {
                    if (AccurevStatus.looselyMatches(new URI(remote), payload.getUrl())) {
                        if(accurevSCMSource.getOwner() != null && type.equals(Type.UPDATED)) {
                            if (AccurevStatus.looselyMatches(accurevSCMSource.getOwner().getAllJobs(), payload.getStream())) {
                                System.out.println("Sending a head event to start building with updated for stream: " + payload.getStream());
                                AccurevSCMHead head = new AccurevSCMHead(payload.getStream());
                                AccurevSCMRevision revision = new AccurevSCMRevision(head, Long.parseLong(payload.getTransaction()));
                                head.setHash(Long.parseLong(payload.getTransaction()));
                                return Collections.<SCMHead, SCMRevision>singletonMap(head, revision);
                            }
                        }
                        else if (type.equals(Type.CREATED)) {
                            System.out.println("Sending a head event to start building in else block for stream " + payload.getStream());
                            AccurevSCMHead head = new AccurevSCMHead(payload.getStream());
                            AccurevSCMRevision revision = new AccurevSCMRevision(head, Long.parseLong(payload.getTransaction()));
                            head.setHash(Long.parseLong(payload.getTransaction()));

                            return Collections.<SCMHead, SCMRevision>singletonMap(head, revision);
                        }
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public boolean isMatch(@NonNull SCM scm) {
        if (scm instanceof AccurevSCM) {
            AccurevSCM accurev = (AccurevSCM) scm;
            List<StreamSpec> streams = accurev.getStreams();
            for (StreamSpec ss : streams) {
                if (ss.getName().equals(payload.getStream())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isMatch(@NonNull SCMSource source) {
        return super.isMatch(source);
    }
}
