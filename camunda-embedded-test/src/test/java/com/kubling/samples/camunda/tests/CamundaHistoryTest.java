package com.kubling.samples.camunda.tests;

import com.kubling.samples.camunda.AbstractCamundaIntegrationTest;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CamundaHistoryTest extends AbstractCamundaIntegrationTest {

    @Test
    void shouldRecordFinishedProcess() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("Process_with_wait");
        runtimeService.deleteProcessInstance(instance.getId(), "test-complete");

        HistoricProcessInstance history = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId())
                .finished()
                .singleResult();

        assertThat(history).isNotNull();
        assertThat(history.getDeleteReason()).isEqualTo("test-complete");
    }
}
