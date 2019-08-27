package jenkins.plugins.accurev;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMHead;

public class AccurevSCMHead extends SCMHead {
    public AccurevSCMHead(@NonNull String name) {
        super(name);
    }
}
