package net.earthmc;

import net.earthmc.common.ItemStack_Serialiser;
import net.earthmc.models.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;

import java.sql.*;

public class db {
    private Connection conn;

    // SQL statements
    private String SQL_createTable = "CREATE TABLE IF NOT EXISTS \"sign_chest\" ("
            + "\"sign_x\"	  INTEGER NOT NULL,"
            + "\"sign_y\"	  INTEGER NOT NULL,"
            + "\"sign_z\"	  INTEGER NOT NULL,"
            + "\"chest_x\"	  INTEGER NOT NULL,"
            + "\"chest_y\"	  INTEGER NOT NULL,"
            + "\"chest_z\"	  INTEGER NOT NULL,"
            + "\"owner\"	  TEXT NOT NULL,"
            + "\"item_stack\" TEXT NOT NULL,"
            + "\"price\"      INTEGER NOT NULL,"
            + "PRIMARY KEY(\"owner\"));";

    // Create a unique index so there can't be multiple signs/chest in the same block
    private String SQL_sign_ix ="CREATE UNIQUE INDEX IF NOT EXISTS ix_sign "
            + "ON sign_chest(sign_x, sign_y, sign_z);";
    private String SQL_chest_ix = "CREATE UNIQUE INDEX IF NOT EXISTS ix_chest "
            + "ON sign_chest(chest_x, chest_y, chest_z)";

    private String SQL_add_change = "REPLACE INTO sign_chest VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private String SQL_get_chest = "SELECT chest_x, chest_y, chest_z FROM sign_chest WHERE sign_x = ? AND sign_y = ? AND sign_z = ?;";
    private String SQL_get_sign = "SELECT sign_x, sign_y, sign_z FROM sign_chest WHERE chest_x = ? AND chest_y = ? AND chest_z = ?;";
    private String SQL_get_item = "SELECT item_stack FROM sign_chest WHERE sign_x = ? AND sign_y = ? AND sign_z = ?;";
    private String SQL_remove_sign = "DELETE FROM sign_chest WHERE sign_x = ? AND sign_y = ? AND sign_z = ?;";
    private String SQL_remove_chest = "DELETE FROM sign_chest WHERE chest_x = ? AND chest_y = ? AND chest_z = ?;";
    private String SQL_get_price = "SELECT price FROM sign_chest WHERE sign_x = ? AND sign_y = ? AND sign_z = ?;";
    private String SQL_get_owner = "SELECT owner FROM sign_chest WHERE sign_x = ? AND sign_y = ? AND sign_z = ?;";

    private ItemStack_Serialiser serialiser;

    // Setup the database connection
    public db(String data_folder, String db_name) throws SQLException {
        // Open the connection and allow semicolon separated queries enabled
        conn = DriverManager.getConnection("jdbc:sqlite:" + data_folder + "\\" + db_name + ".db");

        // For converting the item stack to and from the db
        serialiser = new ItemStack_Serialiser();

        // Create the table if it doesn't exist
        Statement stmt = conn.createStatement();
        stmt.execute(SQL_createTable);
        stmt.execute(SQL_sign_ix);
        stmt.execute(SQL_chest_ix);
        stmt.close();
    }

    // Close the db and clean up
    public void close() throws SQLException {
        // Close the db connection
        conn.close();
    }

    // Add new sign or change existing sign
    public void add_sign(location sign, location chest, String owner, String item_stack, int price) throws SQLException {
        // Create the prepared statement
        PreparedStatement stmt = conn.prepareStatement(SQL_add_change);

        // Add in the values
        stmt.setInt(1, sign.x);
        stmt.setInt(2, sign.y);
        stmt.setInt(3, sign.z);
        stmt.setInt(4, chest.x);
        stmt.setInt(5, chest.y);
        stmt.setInt(6, chest.z);
        stmt.setString(7, owner);
        stmt.setString(8, item_stack);
        stmt.setInt(9, price);

        // Run the query
        stmt.executeUpdate();
        stmt.close();
    }

    // Get the chest a sign links to
    public location get_chest(location sign) throws SQLException {
        // Create the prepared statement
        PreparedStatement stmt = conn.prepareStatement(SQL_get_chest);

        // Fill the values
        stmt.setInt(1, sign.x);
        stmt.setInt(2, sign.y);
        stmt.setInt(3, sign.z);

        // Run the query
        ResultSet results = stmt.executeQuery();

        // Return the location
        location chest = new location(0,0,0);

        try {
            chest.x = results.getInt("chest_x");
            chest.y = results.getInt("chest_y");
            chest.z = results.getInt("chest_z");
        } catch (SQLException e) {
            chest = null;
        }

        // Clean up
        stmt.close();
        results.close();

        return chest;
    }

    // Get the sign a chest links to
    public location get_sign(location chest) throws SQLException {
        // Create the prepared statement
        PreparedStatement stmt = conn.prepareStatement(SQL_get_sign);

        // Fill the values
        stmt.setInt(1, chest.x);
        stmt.setInt(2, chest.y);
        stmt.setInt(3, chest.z);

        // Run the query
        ResultSet results = stmt.executeQuery();

        // Return the location
        location sign = new location(0,0,0);

        try {
            sign.x = results.getInt("sign_x");
            sign.y = results.getInt("sign_y");
            sign.z = results.getInt("sign_z");
        } catch (SQLException e) {
            sign = null;
        }

        // Clean up
        stmt.close();
        results.close();

        return sign;
    }

    public ItemStack get_item(location sign) throws SQLException, InvalidConfigurationException {
        // Create the prepared statement
        PreparedStatement stmt = conn.prepareStatement(SQL_get_item);

        // Fill the values
        stmt.setInt(1, sign.x);
        stmt.setInt(2, sign.y);
        stmt.setInt(3, sign.z);

        // Run the query
        ResultSet results = stmt.executeQuery();

        String item_stack = null;
        try {
            item_stack = results.getString("item_stack");
        } catch (SQLException e) {
            return null;
        }

        stmt.close();
        results.close();

        return serialiser.deserialise(item_stack);
    }

    // Remove by sign
    public void removeSign(location sign) throws SQLException {
        // Create the prepared statement
        PreparedStatement stmt = conn.prepareStatement(SQL_remove_sign);

        // Fill the values
        stmt.setInt(1, sign.x);
        stmt.setInt(2, sign.y);
        stmt.setInt(3, sign.z);

        stmt.executeUpdate();
        stmt.close();
    }

    // Remove by chest
    public void removeChest(location chest) throws SQLException {
        // Create the prepared statement
        PreparedStatement stmt = conn.prepareStatement(SQL_remove_chest);

        // Fill the values
        stmt.setInt(1, chest.x);
        stmt.setInt(2, chest.y);
        stmt.setInt(3, chest.z);

        stmt.executeUpdate();
        stmt.close();
    }

    public int get_price(location sign) throws SQLException {
        // Create the prepared statement
        PreparedStatement stmt = conn.prepareStatement(SQL_get_price);

        // Fill the values
        stmt.setInt(1, sign.x);
        stmt.setInt(2, sign.y);
        stmt.setInt(3, sign.z);

        // Run the query
        ResultSet results = stmt.executeQuery();

        int price = -1;
        try {
            price = results.getInt("price");
        } catch (SQLException e) {
            return -1;
        }

        stmt.close();
        results.close();

        return price;
    }

    public String get_owner(location sign) throws SQLException {
        // Create the prepared statement
        PreparedStatement stmt = conn.prepareStatement(SQL_get_owner);

        // Fill the values
        stmt.setInt(1, sign.x);
        stmt.setInt(2, sign.y);
        stmt.setInt(3, sign.z);

        // Run the query
        ResultSet results = stmt.executeQuery();

        String owner = null;
        try {
            owner = results.getString("owner");
        } catch (SQLException e) {
            return null;
        }

        stmt.close();
        results.close();

        return owner;
    }
}
