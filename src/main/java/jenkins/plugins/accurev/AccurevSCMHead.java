package jenkins.plugins.accurev;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import jenkins.scm.api.SCMHead;

public class AccurevSCMHead extends SCMHead {
    private AccurevTransaction transaction;
    private long hash;
    
    public AccurevSCMHead(@NonNull String name) {
        super(name);
    }

    public long getHash() {
        return hash;
    }

    public void setHash(long hash) {
        this.hash = hash;
    }

    public AccurevTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(AccurevTransaction transaction) {
        this.transaction = transaction;
    }






}
