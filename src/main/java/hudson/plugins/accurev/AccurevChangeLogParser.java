package hudson.plugins.accurev;

import hudson.model.Run;
import hudson.scm.ChangeLogParser;
import hudson.scm.RepositoryBrowser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AccurevChangeLogParser extends ChangeLogParser {

    public AccurevChangeLogParser(){
        super();
    }

    public void parse(InputStream changelog) throws IOException {
    }

    @Override
    public AccurevChangeSetList parse(Run build, RepositoryBrowser<?> browser, File changelogFile) throws IOException{
        LineIterator lineIterator = null;
        try {
            lineIterator = FileUtils.lineIterator(changelogFile, "UTF-8");
            return new AccurevChangeSetList(build, browser, parse(lineIterator));
        } finally{
            LineIterator.closeQuietly(lineIterator);
        }
    }

    private List<AccurevChangeSet> parse(LineIterator changelog) {
        Set<AccurevChangeSet> r = new LinkedHashSet<>();
        List<String> lines = null;
        while(changelog.hasNext()){
            String line = changelog.next();
            if(line.startsWith("transaction: ")) {
                if (lines != null) {
                    r.add(parseTransaction(lines));
                }
                lines = new ArrayList<>();
            }
            if(lines != null) lines.add(line);
        }
        if (lines != null) {
            r.add(parseTransaction(lines));
        }
        return new ArrayList<>(r);
    }

    private AccurevChangeSet parseTransaction(List<String> lines) {
        return new AccurevChangeSet(lines);
    }
}
