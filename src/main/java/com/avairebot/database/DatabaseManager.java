package com.avairebot.database;

import com.avairebot.AvaIre;
import com.avairebot.contracts.database.Database;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.connections.MySQL;
import com.avairebot.database.connections.SQLite;
import com.avairebot.database.exceptions.DatabaseException;
import com.avairebot.database.migrate.Migrations;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.database.schema.Schema;
import com.avairebot.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.WillClose;
import java.sql.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final AvaIre avaire;
    private final Schema schema;
    private final Migrations migrations;

    private Database connection = null;

    public DatabaseManager(AvaIre avaire) {
        this.avaire = avaire;
        this.schema = new Schema(this);
        this.migrations = new Migrations(this);
    }

    public AvaIre getAvaire() {
        return avaire;
    }

    public Schema getSchema() {
        return schema;
    }

    public Migrations getMigrations() {
        return migrations;
    }

    public Database getConnection() throws SQLException, DatabaseException {
        if (connection == null) {
            switch (avaire.getConfig().getString("database.type", "invalid").toLowerCase()) {
                case "mysql":
                    connection = new MySQL(this);
                    break;

                case "sqlite":
                    connection = new SQLite(this);
                    break;

                default:
                    throw new DatabaseException("Invalid database type given, failed to create a new database connection.");
            }

            connection.setDatabaseManager(this);
        }

        if (connection.isOpen()) {
            return connection;
        }

        if (!connection.open()) {
            throw new DatabaseException("Failed to connect to the database.");
        }

        return connection;
    }

    public QueryBuilder newQueryBuilder() {
        return new QueryBuilder(this);
    }

    public QueryBuilder newQueryBuilder(String table) {
        return new QueryBuilder(this, table);
    }

    /**
     * Executes the given SQL statement, which returns a single
     * <code>Collection</code> object.
     *
     * @param query an SQL statement to be sent to the database, typically a
     *              static SQL <code>SELECT</code> statement
     * @return a <code>Collection</code> object that contains the data produced
     * by the given query; never <code>null</code>
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed <code>Statement</code>, the given
     *                      SQL statement produces anything other than a single
     *                      <code>ResultSet</code> object, the method is called on a
     *                      <code>PreparedStatement</code> or <code>CallableStatement</code>
     */
    @WillClose
    public Collection query(String query) throws SQLException {
        log.debug(String.format("query(String query) was called with the following SQL query.\nSQL: %s", query));
        MDC.put("query", query);

        try (ResultSet resultSet = getConnection().query(query)) {
            return new Collection(resultSet);
        }
    }

    /**
     * Executes the SQL statement generated by the query builder, which returns a single
     * <code>Collection</code> object.
     *
     * @param query a QueryBuilder instance that should be sent to the database, typically a
     *              static SQL <code>SELECT</code> statement
     * @return a <code>Collection</code> object that contains the data produced
     * by the given query; never <code>null</code>
     * @throws SQLException        if a database access error occurs,
     *                             this method is called on a closed <code>Statement</code>, the given
     *                             SQL statement produces anything other than a single
     *                             <code>ResultSet</code> object, the method is called on a
     *                             <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     */
    @WillClose
    public Collection query(QueryBuilder query) throws SQLException {
        return query(query.toSQL());
    }

    /**
     * Generates a prepared statement object and executes the SQL statement, which must be an SQL Data
     * Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing, such as a DDL statement.
     *
     * @param query an SQL statement to be sent to the database, typically a static SQL DML statement
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException        if a database access error occurs;
     *                             this method is called on a closed  <code>PreparedStatement</code>
     *                             or the SQL statement returns a <code>ResultSet</code> object
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     */
    @WillClose
    public int queryUpdate(String query) throws SQLException {
        log.debug(String.format("queryUpdate(String query) was called with the following SQL query.\nSQL: %s", query));
        MDC.put("query", query);

        try (Statement stmt = getConnection().prepare(query)) {
            if (stmt instanceof PreparedStatement) {
                return ((PreparedStatement) stmt).executeUpdate();
            }

            return stmt.executeUpdate(query);
        }
    }

    /**
     * Generates a prepared statement object and executes the SQL statement, which must be an SQL Data
     * Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing, such as a DDL statement.
     *
     * @param query a QueryBuilder instance that should be sent to the database, typically a
     *              static SQL DML statement
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException        if a database access error occurs;
     *                             this method is called on a closed  <code>PreparedStatement</code>
     *                             or the SQL statement returns a <code>ResultSet</code> object
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     */
    @WillClose
    public int queryUpdate(QueryBuilder query) throws SQLException {
        return queryUpdate(query.toSQL());
    }

    /**
     * Generates a prepared statement object and executes the SQL statement, which must be an SQL INSERT
     * statement, such as <code>INSERT</code>; After the query has been executed the prepared statement
     * will be used to generate a set of keys, referring to the IDs of the inserted rows.
     *
     * @param query an SQL statement to be sent to the database, typically a static SQL INSERT statement
     * @return a set of IDs referring to the insert rows
     * @throws SQLException        if a database access error occurs;
     *                             this method is called on a closed  <code>PreparedStatement</code>
     *                             or the SQL statement returns a <code>ResultSet</code> object
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     */
    @WillClose
    public Set<Integer> queryInsert(String query) throws SQLException {
        log.debug(String.format("queryInsert(String query) was called with the following SQL query.\nSQL: %s", query));
        Metrics.databaseQueries.labels("INSERT").inc();
        MDC.put("query", query);

        if (!query.startsWith("INSERT INTO")) {
            throw new DatabaseException("queryInsert was called with a query without an INSERT statement!");
        }

        try (PreparedStatement stmt = getConnection().getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.executeUpdate();

            Set<Integer> ids = new HashSet<>();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                while (keys.next()) {
                    ids.add(keys.getInt(1));
                }
            }

            return ids;
        }
    }

    /**
     * Generates a prepared statement object and executes the SQL statement, which must be an SQL INSERT
     * statement, such as <code>INSERT</code>; After the query has been executed the prepared statement
     * will be used to generate a set of keys, referring to the IDs of the inserted rows.
     *
     * @param queryBuilder a QueryBuilder instance that should be sent to the database, typically a
     *                     static SQL INSERT statement
     * @return a set of IDs referring to the insert rows
     * @throws SQLException        if a database access error occurs;
     *                             this method is called on a closed  <code>PreparedStatement</code>
     *                             or the SQL statement returns a <code>ResultSet</code> object
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     */
    @WillClose
    public Set<Integer> queryInsert(QueryBuilder queryBuilder) throws SQLException {
        String query = queryBuilder.toSQL();
        log.debug("queryInsert(QueryBuilder queryBuilder) was called with the following SQL query.\nSQL: " + query);
        Metrics.databaseQueries.labels("INSERT").inc();
        MDC.put("query", query);

        if (query == null) {
            throw new SQLException("null query was generated, null can not be used as a valid query");
        }

        if (!query.startsWith("INSERT INTO")) {
            throw new DatabaseException("queryInsert was called with a query without an INSERT statement!");
        }

        try (PreparedStatement stmt = getConnection().getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            int preparedIndex = 1;
            for (Map<String, Object> row : queryBuilder.getItems()) {
                for (Map.Entry<String, Object> item : row.entrySet()) {
                    if (item.getValue() == null) {
                        continue;
                    }

                    String value = item.getValue().toString();

                    if (value.startsWith("RAW:") ||
                        value.equalsIgnoreCase("true") ||
                        value.equalsIgnoreCase("false") ||
                        value.matches("[-+]?\\d*\\.?\\d+")) {
                        continue;
                    }

                    stmt.setString(preparedIndex++, value);
                }
            }

            stmt.executeUpdate();

            Set<Integer> ids = new HashSet<>();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                while (keys.next()) {
                    ids.add(keys.getInt(1));
                }
            }

            return ids;
        }
    }
}
