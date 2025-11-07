package com.kubling.samples.camunda.support;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KublingBundleBuilder {

    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String CONFIG_DIR = USER_DIR + "/vdb";
    private static final String OUTPUT_FILE = CONFIG_DIR + "/kubling-camunda-descriptor.zip";

    public static void generateBundle() {

        Path output = Path.of(OUTPUT_FILE);
        if (Files.exists(output)) {
            log.info("Descriptor already exists: {}", output);
            return;
        }

        log.info("Generating Kubling bundle using Docker low-level API...");

        DockerClient dockerClient = DockerClientFactory.instance().client();

        try {
            dockerClient.pullImageCmd("kubling/kubling-cli:latest")
                    .exec(new PullImageResultCallback())
                    .awaitCompletion(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        Volume volume = new Volume("/base");
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(CONFIG_DIR, volume));

        CreateContainerResponse container = dockerClient.createContainerCmd("kubling/kubling-cli:latest")
                .withHostConfig(hostConfig)
                .withCmd("bundle", "genmod", "base/descriptor",
                        "-o", "base/kubling-camunda-descriptor.zip", "--parse")
                .exec();

        String containerId = container.getId();
        dockerClient.startContainerCmd(containerId).exec();

        // Capture and stream logs
        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new FrameConsumerResultCallback() {
                        @Override
                        public void onNext(com.github.dockerjava.api.model.Frame item) {
                            log.info(item.toString().trim());
                        }
                    }).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Wait for completion and check exit code
        var inspect = dockerClient.inspectContainerCmd(containerId).exec();
        Long exitCode = inspect.getState().getExitCodeLong();
        log.info("Kubling CLI exited with code: {}", exitCode);

        if (exitCode == null || exitCode != 0) {
            throw new IllegalStateException("Kubling CLI failed; inspect logs above.");
        }

        if (!Files.exists(output)) {
            throw new IllegalStateException("Descriptor not found at: " + output);
        }

        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        log.info("Descriptor generated successfully: {}", output);
    }
}