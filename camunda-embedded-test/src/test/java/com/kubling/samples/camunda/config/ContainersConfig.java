package com.kubling.samples.camunda.config;

import com.kubling.samples.camunda.support.KublingBundleBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.KublingContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public final class ContainersConfig {

    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String CONFIG_DIR = "vdb";
    private static final String APP_CONFIG = "app-config.yaml";
    private static final String BUNDLE = "kubling-camunda-descriptor.zip";
    private static final String CONTAINER_CONFIG_DIR = "app_data";
    private static final String CONTAINER_APP_CONFIG = "/" + CONTAINER_CONFIG_DIR + "/" + APP_CONFIG;
    private static final String CONTAINER_BUNDLE = "/" + CONTAINER_CONFIG_DIR + "/" + BUNDLE;

    public static final Network network = Network.newNetwork();

    public static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withNetwork(network)
            .withNetworkAliases("mysql")
            .withDatabaseName("camunda_tx")
            .withUsername("root")
            .withPassword("test")
            .withInitScript("runtime-mysql.sql")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withUsername("postgres")
            .withPassword("test")
            .withDatabaseName("camunda_history")
            .withInitScript("history-postgres.sql")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

    public static final KublingContainer<?> kubling =
            new KublingContainer<>()
                    .withNetwork(network)
                    .dependsOn(mysql, postgres)
                    .withEnv(Map.of(
                            "ENABLE_WEB_CONSOLE", "FALSE",
                            "SCRIPT_LOG_LEVEL", "DEBUG",
                            "APP_CONFIG", CONTAINER_APP_CONFIG,
                            "DESCRIPTOR_BUNDLE", CONTAINER_BUNDLE,
                            "RUNNING_IN_KUBLING_ENV", "FALSE",
                            "KUBLING_LOG_LEVEL", "debug",
                            "MYSQL_ADDRESS", "mysql",
                            "POSTGRES_ADDRESS", "postgres"
                    ))
                    .withLogConsumer(new Slf4jLogConsumer(log))
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(String.format("%s/%s/%s", USER_DIR, CONFIG_DIR, APP_CONFIG)),
                            CONTAINER_APP_CONFIG
                    )
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(String.format("%s/%s/%s", USER_DIR, CONFIG_DIR, BUNDLE)),
                            CONTAINER_BUNDLE
                    )
                    .withExposedPorts(KublingContainer.DEFAULT_NATIVE_PORT, KublingContainer.DEFAULT_HTTP_PORT)
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

    private static volatile boolean initialized = false;

    static {
        initialize();
    }

    private ContainersConfig() {
        // prevent instantiation
    }

    private static void initialize() {
        if (initialized) return;
        synchronized (ContainersConfig.class) {
            if (initialized) return;

            KublingBundleBuilder.generateBundle();

            log.info("Starting MySQL, PostgreSQL and Kubling containers...");

            Startables.deepStart(Stream.of(mysql, postgres)).join();

            log.info("MySQL and Postgres started: mysql={} postgres={}",
                    mysql.getJdbcUrl(), postgres.getJdbcUrl());

            Startables.deepStart(Stream.of(kubling)).join();

            log.info("Kubling started on port {}",
                    kubling.getMappedPort(KublingContainer.DEFAULT_NATIVE_PORT));

            initialized = true;
        }
    }

    public static int getKublingPort() {
        initialize();
        return kubling.getMappedPort(KublingContainer.DEFAULT_NATIVE_PORT);
    }

    public static String getMySQLJdbcUrl() {
        initialize();
        return mysql.getJdbcUrl();
    }

    public static String getPostgresJdbcUrl() {
        initialize();
        return postgres.getJdbcUrl();
    }

    public static void shutdown() {
        synchronized (ContainersConfig.class) {
            log.info("Stopping containers...");
            Stream.of(kubling, mysql, postgres)
                    .filter(org.testcontainers.containers.ContainerState::isRunning)
                    .forEach(container -> {
                        try {
                            container.stop();
                        } catch (Exception e) {
                            log.warn("Error stopping container {}: {}", container.getDockerImageName(), e.getMessage());
                        }
                    });
            initialized = false;
        }
    }

}
