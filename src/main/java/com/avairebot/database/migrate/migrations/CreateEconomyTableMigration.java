package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.migrate.Migrations;
import com.avairebot.database.schema.Schema;
import com.avairebot.time.Carbon;

import java.sql.SQLException;
public class CreateEconomyTableMigration implements Migration{
    /**
     * Gets the time the migration was created at, this is used to order
     * migrations, making sure migrations are rolled out to the
     * database and back in the right order.
     * <p>
     * The time format can be any of the supported carbon time formats.
     *
     * @return the carbon time string
     * @see Carbon
     */
    @Override
    public String created_at() {
        return "Thurs, Mar 22, 2012 7:45 PM";
    }

    /**
     * Attempts to migrate the database, this is automatically executed from the
     * {@link Migrations#up() migrate up} method.
     *
     * @param schema the database schematic instance
     * @return the result of the schematic instance call
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed <code>Statement</code>, the given
     *                      SQL statement produces anything other than a single
     *                      <code>ResultSet</code> object, the method is called on a
     *                      <code>PreparedStatement</code> or <code>CallableStatement</code>
     */
    @Override
    public boolean up(Schema schema) throws SQLException
    {
        return schema.createIfNotExists(Constants.ECONOMY_TABLE_NAME, table -> {
            table.Long("guild_id").unsigned();
            table.Long("user_id").unsigned();
            table.Long("user_id").unsigned();
            table.Integer("timesClaimed").defaultValue(0);
            table.Boolean("active").defaultValue(true);
            table.Long("balance").defaultValue(100);
            table.Timestamps();
        });
    }

    /**
     * Attempts to rollback the migrations from the database, this is automatically executed from the
     * {@link Migrations#down() down()} and
     * {@link Migrations#rollback(int) rollback(int)} method.
     *
     * @param schema the database schematic instance
     * @return the result of the schematic instance call
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed <code>Statement</code>, the given
     *                      SQL statement produces anything other than a single
     *                      <code>ResultSet</code> object, the method is called on a
     *                      <code>PreparedStatement</code> or <code>CallableStatement</code>
     */
    @Override
    public boolean down(Schema schema) throws SQLException
    {
        return schema.dropIfExists(Constants.ECONOMY_TABLE_NAME);
    }
}
