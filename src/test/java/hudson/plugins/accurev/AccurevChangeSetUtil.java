package hudson.plugins.accurev;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class AccurevChangeSetUtil {

    static final String TRANSACTION_ID = "405612";
    static final String STREAM_NAME = "dev_testing_staged_stream_12";
    static final String COMMITTER_NAME = "Jane";
    static final String TRANSACTION_TYPE = "Promote";
    static final String COMMITTER_DATE = "2019-07-15 14:11:03.000+0200";


    public static AccurevChangeSet genChangeSet() {

        ArrayList<String> lines = new ArrayList<>();

        lines.add("transaction: " + TRANSACTION_ID);
        lines.add("stream: " + STREAM_NAME);
        lines.add("    file42    ");
        lines.add("Type: " + TRANSACTION_TYPE);
        lines.add("User: " + COMMITTER_NAME);
        lines.add("Time: " + COMMITTER_DATE);
        return new AccurevChangeSet(lines);
    }
}
