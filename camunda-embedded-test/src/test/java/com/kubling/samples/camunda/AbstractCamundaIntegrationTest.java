package com.kubling.samples.camunda;

import com.kubling.samples.camunda.config.ContainersConfig;
import com.kubling.samples.camunda.support.CamundaDeploymentHelper;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SystemStubsExtension.class)
public abstract class AbstractCamundaIntegrationTest {

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected HistoryService historyService;

    @BeforeAll
    static void setupEnvironment() {
        System.setProperty("KUBLING_PORT", String.valueOf(ContainersConfig.getKublingPort()));
    }

    @BeforeEach
    void deployProcesses() {
        CamundaDeploymentHelper.deployIfMissing(repositoryService, "minimal-process", "minimal-process");
        CamundaDeploymentHelper.deployIfMissing(repositoryService, "minimal-process-with-wait", "minimal-process-with-wait");
    }
}
