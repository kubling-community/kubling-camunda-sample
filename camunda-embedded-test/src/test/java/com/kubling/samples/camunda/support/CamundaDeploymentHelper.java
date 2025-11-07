package com.kubling.samples.camunda.support;

import org.camunda.bpm.engine.RepositoryService;

public final class CamundaDeploymentHelper {

    private CamundaDeploymentHelper() {}

    public static void deployIfMissing(RepositoryService repo, String key, String name) {
        long count = repo.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .count();

        if (count == 0) {
            repo.createDeployment()
                    .name(name)
                    .addClasspathResource("processes/%s.bpmn".formatted(name))
                    .deploy();
        }

        verifyDeployment(repo, name);
    }

    public static void verifyDeployment(RepositoryService repo, String name) {
        long count = repo.createProcessDefinitionQuery()
                .processDefinitionName(name)
                .count();

        if (count == 0) {
            throw new IllegalStateException("Process definition '%s' not deployed.".formatted(name));
        }
    }
}
