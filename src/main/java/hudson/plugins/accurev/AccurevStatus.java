package hudson.plugins.accurev;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.Util;
import hudson.model.Cause;
import hudson.model.Item;
import hudson.model.UnprotectedRootAction;
import jenkins.scm.api.SCMEvent;
import jenkins.scm.api.SCMHeadEvent;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.*;

import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;


@Extension
public class AccurevStatus implements UnprotectedRootAction {

    private String lastHost = "";
    private String lastPort = "";
    private String lastStreams = null;
    private String lastTransaction = null;
    private String lastPrincipal = null;
    private Reason lastReason = null;

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Accurev";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "accurev";
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("HOST: ");
        s.append(lastHost);

        s.append(" PORT: ");
        s.append(lastPort);

        if (lastTransaction != null) {
            s.append(" Transaction: ");
            s.append(lastTransaction);
        }
        if (lastStreams != null) {
            s.append(" Streams: ");
            s.append(lastStreams);
        }

        return s.toString();
    }

    public HttpResponse doNotifyCommit(HttpServletRequest request, @QueryParameter(required = true) String host,
                                       @QueryParameter(required = true) String port,
                                       @QueryParameter(required = false) String streams,
                                       @QueryParameter(required = false) String transaction,
                                       @QueryParameter(required = false) String principal,
                                       @QueryParameter(required = false) String reason) throws ServletException, IOException {
        lastHost = host;
        lastPort = port;
        lastStreams = streams;
        lastPrincipal = principal;
        if (reason != null){
            lastReason = Reason.valueOf(reason.toUpperCase());
        }
        URI uri;

        LOGGER.log(Level.FINE, "Received hook from : " + host + ", stream: " + streams);
        try {
            uri = new URI(host + ":" + port);
        } catch (URISyntaxException e) {
            return HttpResponses.error(SC_BAD_REQUEST, new Exception("Illegal Host: " + host + " and port: " + port, e));
        }
        streams = Util.fixEmptyAndTrim(streams);

        String[] streamsArray;
        if (streams == null) {
            streamsArray = new String[0];
        } else {
            streamsArray = streams.split(",");
        }
        String origin = SCMEvent.originOf(request);
        if (streamsArray.length > 0) {
            for (String stream : streamsArray) {
                switch (lastReason != null ? lastReason : Reason.NONE ) {
                    case CREATED:
                        if (StringUtils.isNotBlank(stream) ) {
                            transaction = transaction.isEmpty() ? transaction : "1";
                            SCMHeadEvent.fireNow(new AccurevSCMHeadEvent<String>(
                                    SCMEvent.Type.CREATED, new AccurevCommitPayload(uri, stream, transaction), origin));
                            return HttpResponses.ok();
                        }
                    case UPDATED:
                        if (StringUtils.isNotBlank(stream) && StringUtils.isNotBlank(transaction)) {
                            System.out.println("notifier is updated");
                            SCMHeadEvent.fireNow(new AccurevSCMHeadEvent<String>(
                                    SCMEvent.Type.UPDATED, new AccurevCommitPayload(uri, stream, transaction), origin));
                            return HttpResponses.ok();
                        }
                    case DELETED:
                        if (StringUtils.isNotBlank(stream) ) {
                            transaction = transaction.isEmpty() ? transaction : "1";
                            SCMHeadEvent.fireNow(new AccurevSCMHeadEvent<String>(
                                    SCMEvent.Type.REMOVED, new AccurevCommitPayload(uri, stream, transaction), origin));
                            return HttpResponses.ok();
                        }
                    default:
                        return  HttpResponses.error(408,"No suitable Command found");
                }
            }
        }
        return (staplerRequest, staplerResponse, o) -> {
            staplerResponse.setStatus(SC_OK);
            staplerResponse.setContentType("text/plain");
            staplerResponse.addHeader("Success", "Test Message");
        };
    }

    public static abstract class Listener implements ExtensionPoint {
        public  List<ResponseContributor> onNotifyCommit(String origin,
                                   URI uri,
                                   String transaction,
                                   String... streams) throws MalformedURLException {

            throw new AbstractMethodError();
        }


        public List<StreamSpec> findMatchingStreams(String[] hookStreams, List<StreamSpec> streams) {
            List<StreamSpec> result = new ArrayList();
            for (String hookStream : hookStreams) {
                for (StreamSpec stream : streams) {
                    if (hookStream.equals(stream.getName())) {
                        result.add(stream);
                        break;
                    }
                }
            }
            return result;
        }

        public static class CommitHookCause extends Cause {

            public final String transactionId;

            public CommitHookCause(String transactionId) {
                this.transactionId = transactionId;
            }

            @Override
            public String getShortDescription() {
                return "commit notification " + transactionId;
            }
        }

        private static class PollingScheduledResponseContributor extends ResponseContributor {
            /**
             * The project
             */
            private final Item project;

            /**
             * Constructor.
             *
             * @param project the project.
             */
            public PollingScheduledResponseContributor(Item project) {
                this.project = project;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void addHeaders(StaplerRequest req, StaplerResponse rsp) {
                rsp.addHeader("Triggered", project.getUrl());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void writeBody(PrintWriter w) {
                w.println("Scheduled polling of " + project.getFullDisplayName());
            }
        }

        private static class ScheduledResponseContributor extends ResponseContributor {
            /**
             * The project
             */
            private final Item project;

            /**
             * Constructor.
             *
             * @param project the project.
             */
            public ScheduledResponseContributor(Item project) {
                this.project = project;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void addHeaders(StaplerRequest req, StaplerResponse rsp) {
                rsp.addHeader("Triggered", project.getUrl());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void writeBody(PrintWriter w) {
                w.println("Scheduled " + project.getFullDisplayName());
            }
        }


    }

    public static boolean looselyMatches(URI lhs, URI rhs) {
        return StringUtils.equals(lhs.getHost(),rhs.getHost())
                && StringUtils.equals(lhs.getPath(), rhs.getPath());
    }

    public static class ResponseContributor {
        /**
         * Add headers to the response.
         *
         * @param req the request.
         * @param rsp the response.
         * @since 1.4.1
         */
        public void addHeaders(StaplerRequest req, StaplerResponse rsp) {
        }

        /**
         * Write the contributed body.
         *
         * @param req the request.
         * @param rsp the response.
         * @param w   the writer.
         * @since 1.4.1
         */
        public void writeBody(StaplerRequest req, StaplerResponse rsp, PrintWriter w) {
            writeBody(w);
        }

        /**
         * Write the contributed body.
         *
         * @param w the writer.
         * @since 1.4.1
         */
        public void writeBody(PrintWriter w) {
        }
    }

    public static class MessageResponseContributor extends ResponseContributor {
        /**
         * The message.
         */
        private final String msg;

        /**
         * Constructor.
         *
         * @param msg the message.
         */
        public MessageResponseContributor(String msg) {
            this.msg = msg;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeBody(PrintWriter w) {
            w.println(msg);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(AccurevStatus.class.getName());
}
