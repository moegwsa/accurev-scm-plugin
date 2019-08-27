package hudson.plugins.accurev.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Result;
import jenkins.plugins.accurevclient.Accurev;
import jenkins.plugins.accurevclient.model.AccurevStream;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import org.kohsuke.stapler.export.ExportedBean;


import java.io.Serializable;
import java.util.*;

@ExportedBean(defaultVisibility = 998)
public class Build implements Serializable, Cloneable {
    public HashSet<Object> remoteStreams;

    @SuppressFBWarnings(value="SE_BAD_FIELD", justification="Known non-serializable field")
    public AccurevStream marked;

    @SuppressFBWarnings(value="SE_BAD_FIELD", justification="Known non-serializable field")
    public AccurevTransaction transaction;

    public Collection<AccurevTransaction> getUpdatesSincePrevBuild() {
        return (updatesSincePrevBuild != null) ? updatesSincePrevBuild : new ArrayList<>();
    }

    public void setUpdatesSincePrevBuild(List<AccurevTransaction> updatesSincePrevBuild) {
        this.updatesSincePrevBuild = updatesSincePrevBuild;
    }

    @SuppressFBWarnings(value="SE_BAD_FIELD", justification="Known non-serializable field")
    public Collection<AccurevTransaction> updatesSincePrevBuild;
    public int      hudsonBuildNumber;
    public Result hudsonBuildResult;

    public Build(AccurevStream marked, AccurevTransaction transaction, Collection<AccurevTransaction> updatesSincePrevBuild, int hudsonBuildNumber, Result hudsonBuildResult) {
        this.marked = marked;
        this.transaction = transaction;
        this.hudsonBuildNumber = hudsonBuildNumber;
        this.hudsonBuildResult = hudsonBuildResult;
        this.updatesSincePrevBuild = updatesSincePrevBuild;
    }

    public Build(AccurevTransaction at, AccurevStream as, int buildNumber, Result result) {
        this(as, at, null, buildNumber, result);
    }

    public AccurevTransaction getTransaction() {
        return transaction;
    }



    public void setTransactionId(AccurevTransaction transaction) {
        this.transaction = transaction;
    }

    public AccurevStream getMarked() {
        return marked;
    }

    public String getComment() {
        String comment = this.transaction.getComment().isEmpty() ? this.transaction.getType().getType() : this.transaction.getComment();
        return comment;
    }

    @Override
    public Build clone() {
        Build clone;
        try {
            clone = (Build) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException("Error cloning Build", e);
        }

        return clone;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hudsonBuildNumber, transaction, marked);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Build that = (Build) o;

        return hudsonBuildNumber == that.hudsonBuildNumber
                && Objects.equals(transaction, that.transaction)
                && Objects.equals(marked, that.marked);
    }
}
