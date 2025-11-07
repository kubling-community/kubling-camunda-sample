package com.kubling.samples.camunda.tx;

import org.camunda.bpm.engine.impl.cfg.TransactionContext;
import org.camunda.bpm.engine.impl.cfg.TransactionListener;
import org.camunda.bpm.engine.impl.cfg.TransactionState;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;

import java.util.ArrayList;
import java.util.List;

public class KublingTransactionContext implements TransactionContext {

    private final CommandContext commandContext;
    private static final ThreadLocal<Boolean> txActive = ThreadLocal.withInitial(() -> false);

    private boolean newTransactionStarted = false;
    private final List<TransactionListener> commitListeners = new ArrayList<>();
    private final List<TransactionListener> rollbackListeners = new ArrayList<>();

    public KublingTransactionContext(CommandContext commandContext) {
        this.commandContext = commandContext;
        // only start if not already participating
        if (!txActive.get()) {
            // mimic "begin"
            txActive.set(true);
            newTransactionStarted = true;
        }
    }

    @Override
    public void commit() {
        if (newTransactionStarted) {
            // mimic "commit"
            for (TransactionListener listener : commitListeners) {
                listener.execute(commandContext);
            }
            txActive.remove(); // reset only if we started it
        }
    }

    @Override
    public void rollback() {
        if (newTransactionStarted) {
            // mimic "rollback"
            for (TransactionListener listener : rollbackListeners) {
                listener.execute(commandContext);
            }
            txActive.remove();
        }
    }

    @Override
    public void addTransactionListener(TransactionState transactionState, TransactionListener listener) {
        switch (transactionState) {
            case COMMITTED -> commitListeners.add(listener);
            case ROLLED_BACK -> rollbackListeners.add(listener);
        }
    }

    @Override
    public boolean isTransactionActive() {
        return txActive.get();
    }
}