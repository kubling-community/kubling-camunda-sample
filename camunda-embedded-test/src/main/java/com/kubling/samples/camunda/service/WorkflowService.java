package com.kubling.samples.camunda.service;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {

    @Autowired
    private RuntimeService runtimeService;

    @Transactional
    public void startProcess(String processDefinitionKey) {
        runtimeService.startProcessInstanceByKey(processDefinitionKey);
    }
}