package com.kubling.samples.camunda.tests;

import com.kubling.samples.camunda.AbstractCamundaIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class CamundaQueryTest extends AbstractCamundaIntegrationTest {

    @Test
    void shouldListMultipleHistoricProcesses() {
        for (int i = 0; i < 3; i++) {
            ProcessInstance instance = runtimeService.startProcessInstanceByKey("Process_1kaebg3");
            assertThat(instance.getId()).isNotNull();
        }

        List<HistoricProcessInstance> historyList = historyService
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey("Process_1kaebg3")
                .finished()
                .list();

        assertThat(historyList).hasSizeGreaterThanOrEqualTo(3);

        for (final var h : historyList) {
            log.debug(h.getStartTime().toString());
            log.debug(h.getEndTime().toString());
            log.debug(h.getState());
        }
    }
}
