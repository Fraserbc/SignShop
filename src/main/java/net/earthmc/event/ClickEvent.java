package net.earthmc.event;

import net.earthmc.db;
import net.earthmc.models.*;
import net.earthmc.common.*;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static org.bukkit.Bukkit.getServer;
import static org.bukkit.Bukkit.getWorld;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClickEvent implements Listener {
    private Vector<ShopSign> signs;
    private InventoryEvent InvEvent;
    private ItemStack_Serialiser serialiser;
    private SignEnum sign_enum;
    private db database;
    private Economy econ;

    public ClickEvent(Vector<ShopSign> signs, db database, Economy econ) {
        this.InvEvent = new InventoryEvent(null);
        this.signs = signs;
        serialiser = new ItemStack_Serialiser();
        sign_enum = new SignEnum();
        this.database = database;
        this.econ = econ;
    }

    // Detect if a sign or chest was broken
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) throws SQLException {
        Player p = (Player) e.getPlayer();
        Block b = (Block) e.getBlock();

        // If it was a chest
        if(b.getBlockData().getAsString().contains("chest")) {
            location chest = new location(b.getX(), b.getY(), b.getZ());

            location sign = null;
            try {
                sign = database.get_sign(chest);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            if(sign != null) {
                // Remove the sign text
                Sign sing_block = (Sign)(new Location(e.getPlayer().getWorld(), sign.x, sign.y, sign.z).getBlock().getState());
                for(int x = 0; x < 4; x++) {
                    sing_block.setLine(x, "");
                }
                sing_block.update();

                database.removeSign(sign);
            }
        }

        // If it was a sign
        if(b.getBlockData().getAsString().contains("sign")) {
            location sign = new location(b.getX(), b.getY(), b.getZ());

            location chest = null;
            try {
                chest = database.get_chest(sign);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            if(chest != null) {
                database.removeChest(chest);
            }
        }
    }

    // When the player clicks on a sign
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) throws InvalidConfigurationException {
        // Get the block
        Block b = e.getClickedBlock();

        // Check if it was the player right clicking on a block
        if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Check if they have a sign in progress
            ShopSign cur = null;
            for( ShopSign sign : signs) {
                if(sign.owner.equalsIgnoreCase(e.getPlayer().getUniqueId().toString())) {
                    cur = sign;
                    break;
                }
            }

            // Check if they have a sign in progress
            if(cur != null) {
                // Check if it is done, waiting to be sent to the database
                if(cur.done) {
                    return;
                }

                // Check if they clicked on a chest
                if(b.getBlockData().getAsString().contains("chest")) {
                    // Set chest location
                    cur.chest = new location(0,0,0);
                    cur.chest.x = (int)b.getLocation().getX();
                    cur.chest.y = (int)b.getLocation().getY();
                    cur.chest.z = (int)b.getLocation().getZ();

                    // Get the sign
                    Location sign_block_loc = new Location(e.getPlayer().getWorld(),
                            cur.sign.x,
                            cur.sign.y,
                            cur.sign.z
                    );
                    Sign sign_block = (Sign) sign_block_loc.getBlock().getState();

                    sign_block.setLine(0, "§9[item]");
                    sign_block.setLine(1, "§f" + sign_enum.enumToSign(cur.item.getType().name()));
                    sign_block.setLine(2, "§3x" + String.valueOf(cur.item.getAmount()) + " : §6" + String.valueOf(cur.price) + "G");
                    sign_block.setLine(3, cur.selling ? "§cSell" : "§aBuy");
                    sign_block.update();

                    cur.done = true;
                }

                return;
            }

            // Check if the block was a sign
            if(b.getBlockData().getAsString().contains("sign")) { ;
                // Get the sign's text
                Sign sign = (Sign) b.getState();
                String[] lines = sign.getLines();

                // Check if it has been setup yet
                if(lines[0].contains("[item]")) {
                    // Check if all the other lines were empty
                    boolean empty = true;
                    for(int i = 1; i < lines.length; i++) {
                        if(!lines[i].trim().isEmpty()) {
                            empty = false;
                            break;
                        }
                    }

                    // If it was empty, it has not been setup
                    if(empty) {
                        // Check if they have nothing in their hand
                        if(e.getItem() == null) {
                            return;
                        }

                        // Get the item
                        ItemStack item;
                        item = e.getItem().clone();
                        item.setAmount(1);

                        // Check if it has been renamed
                        ItemMeta meta = item.getItemMeta();
                        if(meta.hasDisplayName()) {
                            return;
                        }

                        location sign_loc = new location(0,0,0);
                        sign_loc.x = (int)b.getLocation().getX();
                        sign_loc.y = (int)b.getLocation().getY();
                        sign_loc.z = (int)b.getLocation().getZ();

                        // Build the shop sign
                        ShopSign new_sign = new ShopSign(System.currentTimeMillis() + 30000,
                                sign_loc,
                                null,
                                e.getPlayer().getUniqueId().toString(),
                                item,
                                1,
                                true
                        );

                        // Add the sign to the list of in progress signs
                        signs.add(new_sign);

                        // Add the item to the inventory the player will open
                        Inventory inv = InvEvent.getSellBuy();
                        inv.setItem(4, item);

                        // Get the player to open the inventory
                        e.getPlayer().openInventory(inv);
                    }

                    // If it wasn't empty it might have been setup
                    if(!empty) {
                        // Get the item
                        Location loc = b.getLocation();
                        location sign_loc = new location((int)loc.getX(), (int)loc.getY(), (int)loc.getZ());

                        ItemStack item = null;
                        try {
                            item = new ItemStack(database.get_item(sign_loc));
                        } catch (NullPointerException | IllegalArgumentException ex) {
                            // Do nothing
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }

                        if(item == null) return;

                        // Get linked chest
                        Inventory chest = null;
                        try {
                            location chest_loc = database.get_chest(sign_loc);
                            chest = ((Chest)new Location(e.getPlayer().getWorld(), chest_loc.x, chest_loc.y, chest_loc.z).getBlock().getState()).getInventory();
                        } catch (NullPointerException | IllegalArgumentException ex) {
                            // Do nothing
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }

                        if(chest == null) return;

                        // Get the price
                        int price = -1;
                        try {
                            price = database.get_price(sign_loc);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }

                        if(price == -1) return;

                        // Get the owner
                        Player owner = null;
                        try {
                            owner = getServer().getPlayer(UUID.fromString(database.get_owner(sign_loc)));
                        } catch (SQLException | NullPointerException ex) {
                            ex.printStackTrace();
                        }

                        if(owner == null) return;

                        // Check if it was a buy or sell sign
                        if(sign.getLine(3).contains("Sell")) {
                            // Check if the chest contains the item
                            if(!chest.containsAtLeast(item, item.getAmount())) return;

                            // Check if the user has the amount of money to buy it
                            if(!econ.has(e.getPlayer(), (double)price)) return;

                            // Check if there is enough space for an item to be added
                            if(!e.getPlayer().getInventory().addItem(item).isEmpty()) return;

                            // Pay the owner
                            econ.depositPlayer(owner, price);

                            // Take the players money and the item from the chest
                            econ.withdrawPlayer(e.getPlayer(), price);
                            chest.removeItem(item);
                        }
                        else if(sign.getLine(3).contains("Buy")) {
                            // Check if the player has the item
                            if(!e.getPlayer().getInventory().containsAtLeast(item, item.getAmount())) return;

                            // Check if the buyer has the amount of money to buy it
                            if(!econ.has(owner, (double)price)) return;

                            // Check if there is enough space for an item to be added
                            if(!chest.addItem(item).isEmpty()) return;

                            // Transfer the money and items
                            econ.depositPlayer(e.getPlayer(), (double)price);
                            econ.withdrawPlayer(owner, (double)price);
                            e.getPlayer().getInventory().removeItem(item);
                        }
                    }
                }
            }
        }
    }
}
