package jenkins.plugins.accurev;

import hudson.ExtensionList;
import jenkins.scm.api.SCMSourceDescriptor;
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;


public class AccurevModernSCMTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void accurevIsModernScm(){
        SCMSourceRetriever.DescriptorImpl descriptor = ExtensionList.lookupSingleton(SCMSourceRetriever.DescriptorImpl.class);
        assertThat(descriptor.getSCMDescriptors(), contains(instanceOf(AccurevSCMSource.DescriptorImpl.class)));
    }
}
