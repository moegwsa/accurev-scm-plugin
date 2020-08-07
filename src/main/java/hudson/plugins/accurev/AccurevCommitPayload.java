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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        AccurevCommitPayload that = (AccurevCommitPayload) obj;

        if (!url.equals(that.url)) {
            return false;
        }
        if (!stream.equals(that.stream)) {
            return false;
        }
        return transaction.equals(that.transaction);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + stream.hashCode();
        result = 31 * result + transaction.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AccurevCommitPayload{" +
                "url='" + url+ '\'' +
                ", branch='" + stream + '\'' +
                ", commitId='" + transaction + '\'' +
                '}';
    }
}
