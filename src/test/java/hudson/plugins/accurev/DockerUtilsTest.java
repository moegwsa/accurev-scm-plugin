package hudson.plugins.accurev;

import com.github.dockerjava.api.DockerClient;
import hudson.plugins.accurev.util.DockerUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class DockerUtilsTest {
    private DockerClient dockerClient;

    @Before
    public void setUp() throws Exception {
        this.dockerClient = DockerUtils.getDockerClient();
    }

    @Test
    public void isRunning() throws Exception{
        assertFalse(DockerUtils.ContainerIsRunning(dockerClient, "vanillaIceCream"));
        assertTrue(DockerUtils.ContainerIsRunning(dockerClient, "accurev"));
    }

    @Test
    public void doesExist() throws Exception{
        assertFalse(DockerUtils.ContainerExists(dockerClient, "vanillaIceCream"));
        assertTrue(DockerUtils.ContainerExists(dockerClient, "accurev"));
    }
}
