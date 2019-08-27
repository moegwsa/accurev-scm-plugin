package jenkins.plugins.accurev;

import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.ServerRemoteConfig;
import hudson.plugins.accurev.extensions.impl.BuildItemsDiscovery;
import hudson.plugins.accurev.extensions.impl.TopStreamDiscovery;
import jenkins.scm.api.SCMHead;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.AllOf.allOf;

public class AccurevSCMBuilderTest {

    private AccurevSCMBuilder<?> instance = new AccurevSCMBuilder(
            new SCMHead("master"),
            null,
            "test:5050",
            "depot",
            null
            );

    @Test
    public void build() {
        AccurevSCM scm = instance.build();
        assertThat(scm.getServerRemoteConfigs(), contains(allOf(
                instanceOf(ServerRemoteConfig.class),
                hasProperty("host", is("test")),
                hasProperty("port", is("5050")),
                hasProperty("credentialsId", is(nullValue()))
        )));
    }

    @Test
    public void testWithCredentials() {
        instance.withCredentials("example-id");
        assertThat(instance.credentialsId(), is("example-id"));
        AccurevSCM scm = instance.build();
        assertThat(scm.getServerRemoteConfigs(), contains(allOf(
                instanceOf(ServerRemoteConfig.class),
                hasProperty("host", is("test")),
                hasProperty("port", is("5050")),
                hasProperty("credentialsId", is("example-id"))
        )));
    }

    @Test
    public void testWithExtension() {
        instance.withExtension(new BuildItemsDiscovery());
        assertThat(instance.extensions(), contains(instanceOf(BuildItemsDiscovery.class)));
        AccurevSCM scm = instance.build();
        assertThat(scm.getServerRemoteConfigs(), contains(allOf(
                instanceOf(ServerRemoteConfig.class),
                hasProperty("host", is("test")),
                hasProperty("port", is("5050")),
                hasProperty("credentialsId", is(nullValue())
        ))));
        assertThat(scm.getExtensions(), contains(
                instanceOf(BuildItemsDiscovery.class)
        ));

        instance.withExtension(new TopStreamDiscovery("master"));
        assertThat(instance.extensions(), contains(
                instanceOf(BuildItemsDiscovery.class),
                allOf(instanceOf(TopStreamDiscovery.class), hasProperty("name", is("master")))
        ));
        scm = instance.build();
        assertThat(scm.getServerRemoteConfigs(), contains(allOf(
                instanceOf(ServerRemoteConfig.class),
                hasProperty("host", is("test")),
                hasProperty("port", is("5050")),
                hasProperty("credentialsId", is(nullValue())
                ))));
        assertThat(scm.getExtensions(), contains(
                instanceOf(BuildItemsDiscovery.class),
                allOf(instanceOf(TopStreamDiscovery.class), hasProperty("name", is("master")))
        ));

        // Repeated call with same extension overwrites existing extensions
        instance.withExtension(new TopStreamDiscovery("develop"));
        assertThat(instance.extensions(), contains(
                instanceOf(BuildItemsDiscovery.class),
                allOf(instanceOf(TopStreamDiscovery.class), hasProperty("name", is("develop")))
        ));
        scm = instance.build();
        assertThat(scm.getServerRemoteConfigs(), contains(allOf(
                instanceOf(ServerRemoteConfig.class),
                hasProperty("host", is("test")),
                hasProperty("port", is("5050")),
                hasProperty("credentialsId", is(nullValue())
                ))));
        assertThat(scm.getExtensions(), contains(
                instanceOf(BuildItemsDiscovery.class),
                allOf(instanceOf(TopStreamDiscovery.class), hasProperty("name", is("develop")))
        ));

    }

}
