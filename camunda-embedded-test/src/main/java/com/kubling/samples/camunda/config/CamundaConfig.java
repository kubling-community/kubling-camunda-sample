package com.kubling.samples.camunda.config;

import com.kubling.samples.camunda.tx.KublingTransactionManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.IsolationLevel;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.UUID;

@Configuration
@Slf4j
public class CamundaConfig {

    @Bean
    public static DataSource createHikariDataSource() {

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:teiid:%s@mm://%s:%s",
                System.getenv().getOrDefault("KUBLING_VDB_NAME", "CamundaVDB"),
                System.getenv().getOrDefault("KUBLING_HOST", "localhost"),
                System.getenv().getOrDefault("KUBLING_PORT", "35482")));
        hikariConfig.setUsername(System.getenv().getOrDefault("KUBLING_USERNAME", "sa"));
        hikariConfig.setPassword(System.getenv().getOrDefault("KUBLING_PASSWORD", "sa"));
        hikariConfig.setDriverClassName("com.kubling.teiid.jdbc.TeiidDriver");

        // Additional HikariCP settings
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setConnectionTimeout(20000);
        hikariConfig.setAutoCommit(false);
        hikariConfig.setTransactionIsolation(IsolationLevel.TRANSACTION_READ_COMMITTED.name());

        return new HikariDataSource(hikariConfig);
    }

    @Bean
    @Primary
    public ProcessEngineConfigurationImpl processEngineConfiguration(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext) {

        KublingProcessEngineConfiguration config = new KublingProcessEngineConfiguration();

        config.setDataSource(dataSource);
        config.setTransactionManager(transactionManager);
        config.setTransactionsExternallyManaged(true);

        // Other settings
        config.setDatabaseType("h2");
        config.setDatabaseSchema("camunda");
        config.setDatabaseTablePrefix("camunda.");
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
        config.setHistoryTimeToLive("P365D");
        config.setIdGenerator(() -> UUID.randomUUID().toString());
        config.setMetricsEnabled(false);
        config.setJobExecutorActivate(false);
        config.setBeans(new SpringBeanFactoryProxyMap(applicationContext));

        return config;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new KublingTransactionManager(dataSource);
    }

}
