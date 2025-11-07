package com.kubling.samples.camunda.db.sql;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.ProcessEngineLogger;
import org.camunda.bpm.engine.impl.QueryOrderingProperty;
import org.camunda.bpm.engine.impl.db.EnginePersistenceLogger;
import org.camunda.bpm.engine.impl.db.sql.*;
import org.camunda.bpm.engine.query.QueryProperty;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MybatisForKublingJoinHelper {

    protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;
    protected static final String DEFAULT_ORDER = "RES.ID_ asc";
    public static Map<String, MyBatisTableMapping> mappings = new HashMap<>();

    static {
        mappings.put(QueryOrderingProperty.RELATION_VARIABLE, new VariableTableMapping());
        mappings.put(QueryOrderingProperty.RELATION_PROCESS_DEFINITION, new ProcessDefinitionTableMapping());
        mappings.put(QueryOrderingProperty.RELATION_CASE_DEFINITION, new CaseDefinitionTableMapping());
        mappings.put(QueryOrderingProperty.RELATION_DEPLOYMENT, new DeploymentTableMapping());
    }

    public static String tableAlias(String relation, int index) {
        if (relation == null) {
            return "RES";
        } else {
            MyBatisTableMapping mapping = getTableMapping(relation);

            if (mapping.isOneToOneRelation()) {
                return mapping.getTableAlias();
            } else {
                return mapping.getTableAlias() + index;
            }
        }
    }

    public static String tableMapping(String relation) {
        MyBatisTableMapping mapping = getTableMapping(relation);

        return mapping.getTableName();
    }

    public static String orderBySelection(QueryOrderingProperty orderingProperty, int index) {
        QueryProperty queryProperty = orderingProperty.getQueryProperty();

        StringBuilder sb = new StringBuilder();

        if (queryProperty.getFunction() != null) {
            sb.append(queryProperty.getFunction());
            sb.append("(");
        }

        sb.append(tableAlias(orderingProperty.getRelation(), index));
        sb.append(".");
        sb.append(queryProperty.getName());

        if (queryProperty.getFunction() != null) {
            sb.append(")");
        }

        return sb.toString();
    }

    public static String orderBy(QueryOrderingProperty orderingProperty, int index) {
        QueryProperty queryProperty = orderingProperty.getQueryProperty();
        // Force outer alias
        return "RES." +
                queryProperty.getName() +
                " " +
                orderingProperty.getDirection().getName();
    }

    public static String defaultOrderBy() {
        return DEFAULT_ORDER;
    }

    protected static MyBatisTableMapping getTableMapping(String relation) {
        MyBatisTableMapping mapping = mappings.get(relation);

        if (mapping == null) {
            throw LOG.missingRelationMappingException(relation);
        }

        return mapping;
    }

}
