package com.kubling.samples.camunda.tests;

import com.kubling.samples.camunda.AbstractCamundaIntegrationTest;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CamundaRuntimeTest extends AbstractCamundaIntegrationTest {

    @Test
    void shouldStartProcessAndWriteHistory() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("Process_1kaebg3");
        assertThat(instance).isNotNull();

        HistoricProcessInstance history = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        assertThat(history).isNotNull();
        assertThat(history.getProcessDefinitionKey()).isEqualTo("Process_1kaebg3");
    }
}
