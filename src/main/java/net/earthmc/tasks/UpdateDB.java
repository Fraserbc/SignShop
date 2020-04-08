package net.earthmc.tasks;

import net.earthmc.common.ItemStack_Serialiser;
import net.earthmc.models.*;
import net.earthmc.db;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getPluginManager;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public class UpdateDB extends BukkitRunnable {
    private db database;
    private Vector<ShopSign> signs;
    private JavaPlugin plugin;
    private ItemStack_Serialiser serialiser;

    public UpdateDB(db database, Vector<ShopSign> signs, JavaPlugin plugin) {
        this.database = database;
        this.signs = signs;
        this.plugin = plugin;
        serialiser = new ItemStack_Serialiser();
    }

    @Override
    public void run() {
        // Loop over them, updating the database and removing signs that are complete
        for (int x = signs.size() - 1; x >= 0; x--) {
            ShopSign sign = signs.get(x);
            try {
                // Check if the sign has expired
                if(sign.timeout < System.currentTimeMillis()) {
                    signs.remove(sign);
                    continue;
                }

                if (sign.done) {
                    try {
                        database.add_sign(sign.sign, sign.chest, sign.owner, serialiser.serialise(sign.item), sign.price);
                    } catch (InvalidConfigurationException e) {
                        e.printStackTrace();
                    }
                    signs.remove(sign);
                }
            } catch (SQLException e) {
                getLogger().severe(e.getMessage());
                getPluginManager().disablePlugin(plugin);
            }
        }
    }
}