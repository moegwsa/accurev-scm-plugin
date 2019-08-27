package hudson.plugins.accurev;

import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import org.kohsuke.stapler.export.Exported;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AccurevChangeSetList extends ChangeLogSet<AccurevChangeSet> {

    private final List<AccurevChangeSet> changeSets;

    AccurevChangeSetList(Run<?, ?> run, RepositoryBrowser<?> browser, List<AccurevChangeSet> logs) {
        super(run, browser);
        Collections.reverse(logs);
        this.changeSets = Collections.unmodifiableList(logs);
        for(AccurevChangeSet log : logs)
            log.setParent(this);
    }


    @Override
    public boolean isEmptySet() {
        return changeSets.isEmpty();
    }

    @Override
    public Iterator<AccurevChangeSet> iterator() {
        return changeSets.iterator();
    }

    public List<AccurevChangeSet> getLogs() {
        return changeSets;
    }

    @Exported
    public String getKind() {
        return "accurev";
    }
}
