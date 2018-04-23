package com.avairebot.contracts.database.grammar;

import java.sql.SQLException;
import java.util.Map;

import com.avairebot.database.DatabaseManager;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.database.schema.Blueprint;

public interface Grammarable {
    public String create(DatabaseManager manager, Blueprint blueprint, Map<String, Boolean> options) throws SQLException;
    
    public String delete(DatabaseManager manager, QueryBuilder query, Map<String, Boolean> options) throws SQLException;
    
    public String insert(DatabaseManager manager, QueryBuilder query, Map<String, Boolean> options) throws SQLException;
    
    public String select(DatabaseManager manager, QueryBuilder query, Map<String, Boolean> options) throws SQLException;
    
    public String update(DatabaseManager manager, QueryBuilder query, Map<String, Boolean> options) throws SQLException;
}
