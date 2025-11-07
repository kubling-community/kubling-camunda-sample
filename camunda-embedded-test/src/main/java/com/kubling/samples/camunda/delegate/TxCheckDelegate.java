package com.kubling.samples.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
public class TxCheckDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
        log.info("Spring TX active: {}", isActive);
    }
}
