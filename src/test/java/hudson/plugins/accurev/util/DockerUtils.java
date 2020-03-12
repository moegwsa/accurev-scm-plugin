package hudson.plugins.accurev.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import hudson.util.IOUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class DockerUtils {


    public static boolean ContainerExists(DockerClient client, String id){
        // creating an empty LinkedList
        Collection<String> list = new LinkedList<String>();
        list.add(id);
        return client.listContainersCmd().withShowAll(true).withNameFilter(list).exec().size() > 0;
    }

    public static boolean ContainerIsRunning(DockerClient client, String id){
        // creating an empty LinkedList
        Collection<String> list = new LinkedList<String>();
        list.add(id);
        if(client.listContainersCmd().withShowAll(true).withNameFilter(list).exec().size() > 0) {
            return client.inspectContainerCmd(id).exec().getState().getRunning();
        } return false;
    }

    @Nullable
    public static byte[] readFileFromContainer(@NonNull final DockerClient _dockerClient,
                                         @NonNull final String _container,
                                         @NonNull final String _outputFile) {
        final InputStream fileStream =_dockerClient
                .copyArchiveFromContainerCmd(_container, _outputFile)
                .exec();
        final TarArchiveInputStream tarIn = new TarArchiveInputStream(fileStream);

        try {
            if (tarIn.getNextEntry() == null) {
                return null;
            }

            return IOUtils.toByteArray(tarIn);
        } catch (IOException _e) {
            return null;
        }
    }

    public static boolean runCommand(@NonNull final DockerClient _dockerClient,
                               @NonNull final String _container,
                               @NonNull final String[] _commandWithArguments) {
        final ExecCreateCmdResponse mExecCreateCmdResponse = _dockerClient
                .execCreateCmd(_container)
                .withAttachStdout(true)
                .withCmd(_commandWithArguments)
                .exec();

        try {
            return _dockerClient
                    .execStartCmd(mExecCreateCmdResponse.getId())
                    .exec(new ExecStartResultCallback() {
                        @Override
                        public void onNext(Frame frame) {
                            super.onNext(frame);
                        }
                    })
                    .awaitCompletion(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // ignore
        }
        return false;
    }

    public static DockerClient getDockerClient() {
    /*
    TLS connection: ...

    DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("tcp://192.168.99.100:2376")
            .withDockerTlsVerify(true)
            .withDockerCertPath("/Users/jon/.docker/machine/machines/default")
            .build();
    */

        final String localDockerHost = SystemUtils.IS_OS_WINDOWS ? "tcp://localhost:2375" : "unix:///var/run/docker.sock";

        final DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(localDockerHost)
                .build();

        return DockerClientBuilder
                .getInstance(config)
                .build();
    }

}
