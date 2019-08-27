package hudson.plugins.accurev;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.Util;
import hudson.model.*;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.triggers.SCMTrigger;
import jenkins.model.Jenkins;
import jenkins.plugins.accurev.AccurevSCMHead;
import jenkins.plugins.accurev.AccurevSCMSource;
import jenkins.scm.api.*;
import jenkins.triggers.SCMTriggerItem;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang.StringUtils.isNotEmpty;


@Extension
public class AccurevStatus implements UnprotectedRootAction {

    private String lastHost = "";
    private String lastPort = "";
    private String lastStreams = null;
    private String lastTransaction = null;
    private String lastPrincipal = null;

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
                                       @QueryParameter(required = false) String principal) throws ServletException, IOException {
        lastHost = host;
        lastPort = port;
        lastStreams = streams;
        lastPrincipal = principal;
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
                if (StringUtils.isNotBlank(stream) && StringUtils.isNotBlank(transaction)) {
                    SCMHeadEvent.fireNow(new AccurevSCMHeadEvent<String>(
                            SCMEvent.Type.UPDATED, new AccurevCommitPayload(uri, stream, transaction), origin));
                    return HttpResponses.ok();
                }

//         Jenkins jenkins = Jenkins.getInstanceOrNull();
//                        else {
//
//                            if (jenkins == null) {
//                                LOGGER.severe("Jenkins.getInstance() is null in AccurevStatus.onNotifyCommit");
//                                return result;
//                            }
//
//                            for (final Item project : jenkins.getAllItems()) {
//                                SCMTriggerItem scmTriggerItem = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(project);
//                                if (scmTriggerItem == null) {
//                                    continue;
//                                }
//                                for (SCM scm : scmTriggerItem.getSCMs()) {
//                                    if (!(scm instanceof AccurevSCM)) {
//                                        continue;
//                                    }
//                                    AccurevSCM accurev = (AccurevSCM) scm;
//                                    scmFound = true;
//
//                                    for (ServerRemoteConfig server : accurev.getServerRemoteConfigs()) {
//                                        boolean serverMatches = false;
//                                        URIish matchedUrl = null;
//                                        if (looselyMatches(server.getUri(), uri)) {
//                                            serverMatches = true;
//                                            matchedUrl = server.getUri();
//                                            break;
//                                        }
//                                    }
//
//                                    boolean streamMatches = false;
//                                    List<StreamSpec> matchingStreams;
//
//
//                                    matchingStreams = findMatchingStreams(streams, accurev.getStreams());
//                                    if (matchingStreams.size() > 0) {
//                                        streamMatches = true;
//                                    }
//
//
//                                    if (!streamMatches) continue;
//
//                                    SCMTrigger trigger = scmTriggerItem.getSCMTrigger();
//
//                                    if (isNotEmpty(transaction)) {
//                                        scmTriggerItem.scheduleBuild2(scmTriggerItem.getQuietPeriod(),
//                                                new CauseAction(new CommitHookCause(transaction)),
//                                                new TransactionParameterAction(transaction, uri));
//                                        result.add(new ScheduledResponseContributor(project));
//                                    } else {
//                                        LOGGER.log(Level.INFO, "Triggering the polling of {0}", project.getFullDisplayName());
//                                        trigger.run();
//                                        result.add(new PollingScheduledResponseContributor(project));
//                                        break;
//                                    }
//                                }
//                            }
//
//                            if (!scmFound) {
//                                result.add(new MessageResponseContributor("No accurev jobs found"));
//                            }
//
//                        }

            }
        }
//
//        try {
//            if(jenkins != null) {
//                Collection<Listener> listeners = jenkins.getExtensionList(Listener.class);
//                if (listeners != null) {
//                    for (Listener listener : listeners) {
//                        listener.onNotifyCommit(origin, uri, transaction, streamsArray);
//                    }
//                }
//            }
//        }catch(Exception e) {
//            e.printStackTrace();
//        }


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
