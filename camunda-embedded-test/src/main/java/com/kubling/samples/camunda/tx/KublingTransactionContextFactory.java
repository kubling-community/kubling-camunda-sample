package com.kubling.samples.camunda.tx;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.TransactionContext;
import org.camunda.bpm.engine.impl.cfg.TransactionContextFactory;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;

@Slf4j
public class KublingTransactionContextFactory implements TransactionContextFactory {
    @Override
    public TransactionContext openTransactionContext(CommandContext commandContext) {
        log.debug("TX Open {}", commandContext.getOperationId());
        return new KublingTransactionContext(commandContext);
    }
}

