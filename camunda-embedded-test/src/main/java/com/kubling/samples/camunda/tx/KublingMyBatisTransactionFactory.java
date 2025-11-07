package com.kubling.samples.camunda.tx;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
public class KublingMyBatisTransactionFactory implements TransactionFactory {

    @Override
    public void setProperties(Properties props) {
        // No properties needed
    }

    @Override
    public Transaction newTransaction(Connection conn) {
        log.debug("NEW Tx - Conn {}", conn.toString());
        return new KublingMyBatisTransaction(conn);
    }

    @Override
    public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
        try {
            log.debug("NEW Tx - DS {}", ds.getConnection().toString());
            Connection conn = ds.getConnection();
            // Do NOT set auto-commit or transaction level; Kubling manages it externally
            return new KublingMyBatisTransaction(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create connection for KublingMyBatisTransaction", e);
        }
    }
}

class KublingMyBatisTransaction implements Transaction {

    private final Connection connection;

    public KublingMyBatisTransaction(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void commit() {
        // No-op — transaction is managed externally
    }

    @Override
    public void rollback() {
        // No-op — transaction is managed externally
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            // Swallow it or log if needed
        }
    }

    @Override
    public Integer getTimeout() {
        return null;
    }
}
