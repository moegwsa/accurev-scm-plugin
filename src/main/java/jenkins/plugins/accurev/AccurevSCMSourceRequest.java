package jenkins.plugins.accurev;

import hudson.model.TaskListener;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceRequest;

public class AccurevSCMSourceRequest extends SCMSourceRequest {

    public AccurevSCMSourceRequest(SCMSource scmSource, AccurevSCMSourceContext accurevSCMSourceContext, TaskListener taskListener) {
        super(scmSource, accurevSCMSourceContext, taskListener);
    }
    
}
