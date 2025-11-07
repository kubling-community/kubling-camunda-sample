package com.kubling.samples.camunda.tests;

import com.kubling.samples.camunda.AbstractCamundaIntegrationTest;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

public class CamundaVariableTest extends AbstractCamundaIntegrationTest {

    @Test
    void shouldPersistAndReadVariablesFromHistory() {
        Map<String, Object> vars = Map.of("customer", "ABC123");
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("Process_1kaebg3", vars);

        HistoricVariableInstance variable = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("customer")
                .singleResult();

        assertThat(variable).isNotNull();
        assertThat(variable.getValue()).isEqualTo("ABC123");
    }
}
