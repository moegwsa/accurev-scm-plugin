package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.ServerRemoteConfig;
import hudson.plugins.accurev.StreamSpec;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.search.Search;
import hudson.search.SearchIndex;
import hudson.security.ACL;
import jenkins.scm.api.*;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;

public class AccurevSCMFileSystemBuilderTest {



    private final String remote = "localhost:5050";
    private AccurevSCMFileSystemBuilder accurevSCMFileSystemBuilder;
    private final StandardCredentials credentials = null;

    @Before
    public void createTelescopeForRemote() {
        accurevSCMFileSystemBuilder = new AccurevSCMFileSystemBuilderImpl(remote);
    }

    @Test
    public void testSupports_StringFalse() {
        AccurevSCMFileSystemBuilder accurevSCMFileSystemBuilder = new AccurevSCMFileSystemBuilderImpl();
        assertFalse(accurevSCMFileSystemBuilder.supports(remote));
    }

    @Test
    public void testBuild() throws Exception {
        AccurevSCMSource source = new AccurevSCMSource("0", remote, null);
        SCMSourceOwner scmSourceOwner =  new SCMSourceOwnerImpl();
        source.setOwner(scmSourceOwner);
        SCMHead head = new SCMHead("some-name");
        Long transactionId = Long.valueOf(1);
        SCMRevision rev = new AccurevSCMSource.SCMRevisionImpl(head,transactionId);
        SCMFileSystem fileSystem = accurevSCMFileSystemBuilder.build(source, head, rev);
        assertThat(fileSystem.getRevision(), is(rev));
        assertThat(fileSystem.isFixedRevision(), is(true));
    }

    @Test
    public void testBuildNoOwner() throws Exception {
        SCMSource source = new AccurevSCMSource("0", remote, null);
        SCMHead head = new SCMHead("some-name");
        SCMRevision rev = null;
        // When source has no owner, build returns null
        assertThat(accurevSCMFileSystemBuilder.build(source, head, rev), is(nullValue()));
    }

    @Test
    public void testGetRevisions() throws Exception {
        SCMHead head = new SCMHead("some-name");
        Long transactionId = Long.valueOf(1);
        SCMRevision rev = new AccurevSCMSource.SCMRevisionImpl(head, transactionId);
        AccurevSCMFileSystemBuilder accurevSCMFileSystemBuilder = new AccurevSCMFileSystemBuilderImpl(remote, rev);
        String stream = "master";
        assertThat(accurevSCMFileSystemBuilder.getRevision(remote, credentials, stream), is(rev));

    }

    @Test
    public void testSupportsSCM() throws Exception {
        AccurevSCM singleStreamSource = getSingleStreamSource(remote);
        assertTrue(accurevSCMFileSystemBuilder.supports(singleStreamSource));
    }

    @Test
    public void testSupportsNullSCM() throws Exception {
        NullSCM nullSCM = new NullSCM();
        assertFalse(accurevSCMFileSystemBuilder.supports(nullSCM));
    }

    @Test
    public void testSupportsSCMSourceNoOwner() {
        SCMSource source = new SCMSourceImpl();
        assertFalse(accurevSCMFileSystemBuilder.supports(source));
    }

    @Test
    public void testTimestamp() throws Exception {
        String stream = "master";
        assertThat(accurevSCMFileSystemBuilder.getTimestamp(remote, credentials, stream), is(12345L));
    }

    private AccurevSCM getSingleStreamSource(String remote) {
        ServerRemoteConfig remoteConfig = new ServerRemoteConfig(
                remote,
                "",
                null
        );
        List<ServerRemoteConfig> serverRemoteConfigList = new ArrayList<>();
        serverRemoteConfigList.add(remoteConfig);
        StreamSpec masterStreamSpec = new StreamSpec("master", "depot");
        List<StreamSpec> streamSpecList = new ArrayList<>();
        streamSpecList.add(masterStreamSpec);
        AccurevSCM singleStreamSource = new AccurevSCM(serverRemoteConfigList,
                streamSpecList, null, null);
        return singleStreamSource;
    }


    private static class AccurevSCMFileSystemBuilderImpl extends AccurevSCMFileSystemBuilder {
        final private String allowedRemote;
        final private SCMRevision revision;
        private List<SCMRevision> revisionList;


        public AccurevSCMFileSystemBuilderImpl(String allowedRemote) {
            this.allowedRemote = allowedRemote;
            this.revision = null;
            revisionList = new ArrayList<>();
        }

        public AccurevSCMFileSystemBuilderImpl() {
            this.allowedRemote = null;
            this.revision = null;
            revisionList = new ArrayList<>();
        }

        public AccurevSCMFileSystemBuilderImpl(String remote, SCMRevision rev) {
            this.allowedRemote = remote;
            this.revision = rev;
            revisionList = new ArrayList<>();
            revisionList.add(this.revision);
        }

        @Override
        public boolean supports(String remote) {
            if (allowedRemote == null) {
                return false;
            }
            return allowedRemote.equals(remote);
        }

        @Override
        public SCMRevision getRevision(String remote, StandardCredentials credentials, String refOrHash) throws IOException, InterruptedException {
            return revision;
        }

        @Override
        public long getTimestamp(String remote, StandardCredentials credentials, String stream) {
            return 12345L;
        }
    }

    private class SCMSourceOwnerImpl implements SCMSourceOwner {
        @NonNull
        @Override
        public List<SCMSource> getSCMSources() {
            return null;
        }

        @Override
        public SCMSource getSCMSource(String s) {
            return null;
        }

        @Override
        public void onSCMSourceUpdated(@NonNull SCMSource scmSource) {

        }

        @Override
        public SCMSourceCriteria getSCMSourceCriteria(@NonNull SCMSource scmSource) {
            return null;
        }

        @Override
        public ItemGroup<? extends Item> getParent() {
            return null;
        }

        @Override
        public Collection<? extends Job> getAllJobs() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getFullName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getFullDisplayName() {
            return null;
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getShortUrl() {
            return null;
        }

        @Override
        public void onLoad(ItemGroup<? extends Item> itemGroup, String s) throws IOException {

        }

        @Override
        public void onCopiedFrom(Item item) {

        }

        @Override
        public void save() throws IOException {

        }

        @Override
        public void delete() throws IOException, InterruptedException {

        }

        @Override
        public File getRootDir() {
            return null;
        }

        @Override
        public Search getSearch() {
            return null;
        }

        @Override
        public String getSearchName() {
            return null;
        }

        @Override
        public String getSearchUrl() {
            return null;
        }

        @Override
        public SearchIndex getSearchIndex() {
            return null;
        }

        @Nonnull
        @Override
        public ACL getACL() {
            return null;
        }
    }

    private class SCMSourceImpl extends SCMSource {

        public SCMSourceImpl() {
        }

        @Override
        protected void retrieve(SCMSourceCriteria scmSourceCriteria, @NonNull SCMHeadObserver scmHeadObserver, SCMHeadEvent<?> scmHeadEvent, @NonNull TaskListener taskListener) throws IOException, InterruptedException {
            throw new UnsupportedOperationException("Not called.");
        }

        @NonNull
        @Override
        public SCM build(@NonNull SCMHead scmHead, SCMRevision scmRevision) {
            throw new UnsupportedOperationException("Not called.");
        }
    }
}
