package jenkins.plugins.accurev;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.trait.SCMSourceContext;

public class AccurevSCMSourceContext<C extends AccurevSCMSourceContext<C, R>, R extends AccurevSCMSourceRequest>
        extends SCMSourceContext<C, R> {

    public String topStream = "";

    private boolean wantStreams;
    private boolean wantWorkspaces;
    private boolean wantSnapshots;
    private boolean wantPassThroughs;
    private boolean wantStagingStreams;


    public AccurevSCMSourceContext(SCMSourceCriteria criteria, @NonNull SCMHeadObserver observer) {
        super(criteria, observer);

    }


    @Override
    public R newRequest(@NonNull SCMSource scmSource, TaskListener taskListener) {
        return (R) new AccurevSCMSourceRequest(scmSource, this, taskListener);
    }


    public boolean isWantStreams() {
        return wantStreams;
    }

    public boolean isWantWorkspaces() {
        return wantWorkspaces;
    }

    public boolean isWantSnapshots() {
        return wantSnapshots;
    }

    public boolean isWantPassThroughs() {
        return wantPassThroughs;
    }

    public boolean iswantStagingStreams() {
        return wantStagingStreams;
    }

    public C wantStreams(boolean include) {
        wantStreams = wantStreams || include;
        return (C) this;
    }

    public C wantWorkspaces(boolean include) {
        wantWorkspaces = wantWorkspaces || include;
        return (C) this;
    }

    public C wantSnapshots(boolean include) {
        wantSnapshots = wantSnapshots || include;
        return (C) this;
    }

    public C topStream(String topStream) {
        this.topStream = topStream.isEmpty() ? this.topStream : topStream;
        return (C) this;
    }

    public C wantStagingStreams(boolean include) {
        wantStagingStreams = wantStagingStreams || include;
        return (C) this;
    }

    public C wantPassThroughs(boolean include) {
        wantPassThroughs = wantPassThroughs || include;
        return (C) this;
    }
}
