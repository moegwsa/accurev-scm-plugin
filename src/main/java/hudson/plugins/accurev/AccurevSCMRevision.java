package hudson.plugins.accurev;

import jenkins.plugins.accurev.AccurevSCMHead;
import jenkins.plugins.accurev.AccurevSCMSource;

public class AccurevSCMRevision extends AccurevSCMSource.SCMRevisionImpl {
    public AccurevSCMRevision(AccurevSCMHead head, Long hash) {
        super(head, hash);
    }
}
