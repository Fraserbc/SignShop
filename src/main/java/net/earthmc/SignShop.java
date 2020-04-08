package net.earthmc;

import net.earthmc.event.*;
import net.earthmc.models.*;
import net.earthmc.tasks.*;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import static org.bukkit.Bukkit.getPluginManager;

import net.milkbowl.vault.economy.Economy;

import java.sql.SQLException;
import java.util.Vector;

public class SignShop extends JavaPlugin {
    private db database;

    private Economy econ;

    private Vector<ShopSign> signs;

    private ClickEvent signEvents;
    private InventoryEvent invEvents;

    // Initialising the API and such
    @Override
    public void onEnable() {
        getLogger().info("SignShop initialising!");

        // Save default config if not exists
        this.saveDefaultConfig();

        // Get the API handle
        if (!setupEconomy() ) {
            getLogger().severe("Couldn't find Vault, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Setup the database
        try {
            database = new db(getDataFolder().toString(),
                    getConfig().getString("db.db_name")
            );
        } catch(SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to initialise the database, disabling...");
            getPluginManager().disablePlugin(this);
            return;
        }

        // Create the List for storing the in-progress signs
        signs = new Vector<ShopSign>();

        // Get the event listeners
        signEvents = new ClickEvent(signs, database, econ);
        invEvents = new InventoryEvent(signs);

        getPluginManager().registerEvents(signEvents, this);
        getPluginManager().registerEvents(invEvents, this);

        // Start the database updater on a loop
        UpdateDB db_scheduler = new UpdateDB(database, signs, this);
        db_scheduler.runTaskTimerAsynchronously(this, 0L, 20L);
    }

    // Get our handle to the Vault API
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("1");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }


    @Override
    public void onDisable() {
        getLogger().info("SignShop eeeeeee");

        // Close the db
        if(database != null) {
            try {
                database.close();
            } catch (SQLException e) {}
        }
    }
}
