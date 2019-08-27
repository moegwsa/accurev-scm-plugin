package hudson.plugins.accurev;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.net.URI;

public class AccurevCommitPayload implements Serializable {
    private final String transaction;
    private final String stream;
    private final URI url;

    public AccurevCommitPayload(@Nonnull URI url, @Nonnull String stream, @Nonnull String transaction) {
        this.url = url;
        this.stream = stream;
        this.transaction = transaction;
    }

    public String getTransaction() {
        return transaction;
    }

    public String getStream() {
        return stream;
    }

    public URI getUrl() {
        return url;
    }
}
