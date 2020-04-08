package net.earthmc.event;

import net.earthmc.models.*;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.bukkit.Bukkit.createInventory;

public class InventoryEvent implements InventoryHolder, Listener {
    private final Inventory sellBuy;
    private final Inventory itemAmount;
    private final Inventory itemPrice;

    private Vector<ShopSign> signs;

    private boolean can_remove = true;

    @Override
    public Inventory getInventory() {
        return itemAmount;
    }

    public Inventory getInventoryAmount() {
        return itemAmount;
    }

    public Inventory getInventoryPrice() {
        return itemPrice;
    }

    public Inventory getSellBuy() { return sellBuy; }

    public InventoryEvent(Vector<ShopSign> signs) {
        this.signs = signs;

        // Create the sell or buy selection menu
        sellBuy = createInventory(this, 9, "Selling or Buying");

        // Create the item amount inventory
        itemAmount = createInventory(this, 9, "Item Amount");

        // Create the item price inventory
        itemPrice = createInventory(this, 9, "Item Price");

        // Exit button
        ItemStack exit = new ItemStack(Material.RED_STAINED_GLASS, 1);
        setDisplayName(exit, "Exit");

        sellBuy.setItem(0, exit);
        itemAmount.setItem(0, exit);
        itemPrice.setItem(0, exit);

        // Next button
        ItemStack next = new ItemStack(Material.GREEN_STAINED_GLASS, 1);
        setDisplayName(next, "Next");

        itemAmount.setItem(8, next);
        itemPrice.setItem(8, next);

        // Selling or buying numbers
        ItemStack buy = new ItemStack(Material.GREEN_WOOL, 1);
        setDisplayName(buy, "Buy");

        ItemStack sell = new ItemStack(Material.RED_WOOL, 1);
        setDisplayName(sell, "Sell");

        sellBuy.setItem(3, sell);
        sellBuy.setItem(5, buy);

        // More and less buttons
        ItemStack more = new ItemStack(Material.GREEN_WOOL, 1);
        setDisplayName(more, "More");

        ItemStack less = new ItemStack(Material.RED_WOOL, 1);
        setDisplayName(less, "Less");

        itemAmount.setItem(3, less);
        itemAmount.setItem(5, more);
        itemPrice.setItem(3, less);
        itemPrice.setItem(5, more);

        // Add gold to the price ui
        ItemStack gold = new ItemStack(Material.GOLD_INGOT, 1);
        itemPrice.setItem(4, gold);
    }

    // Set item display name
    private void setDisplayName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
    }

    // Deep clone an inventory
    private Inventory clone_inv(Inventory inv_original, String title) {
        // Copy name, size and other stuff
        Inventory inv = createInventory(this, inv_original.getSize(), title);
        inv.setContents(inv_original.getContents());

        return inv;
    }

    // Check if our inventory was closed
    @EventHandler
    public void invClose(InventoryCloseEvent e){
        // Check if it is one of our inventories
        if (!(e.getInventory().getHolder() instanceof InventoryEvent)) return;

        // Get the sign in progress
        ShopSign cur = null;
        for( ShopSign sign : signs) {
            if(sign.owner.equalsIgnoreCase(e.getPlayer().getUniqueId().toString())) {
                cur = sign;
                break;
            }
        }

        // Check if cur is null
        if(cur == null) {
            return;
        }

        // If they have an in progress sign, delete it
        if(can_remove) {
            signs.remove(cur);
        }
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // Check if it is one of our inventories
        if (!(e.getInventory().getHolder() instanceof InventoryEvent)) return;

        // Cancel the event so the player can't take the item
        e.setCancelled(true);

        // Check if it was air or null
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        // Get the sign in progress
        ShopSign cur = null;
        for( ShopSign sign : signs) {
            if(sign.owner.equalsIgnoreCase(e.getWhoClicked().getUniqueId().toString())) {
                cur = sign;
                break;
            }
        }

        // Check if cur is null
        if(cur == null) {
            return;
        }

        // Check if the sign is done
        if(cur.done) {
            return;
        }

        // Set the can remove flag to true
        can_remove = true;

        // Get the player
        HumanEntity player = e.getWhoClicked();

        // Get the inventory that was clicked
        Inventory inv = clone_inv(e.getClickedInventory(), e.getView().getTitle());

        // Get the clicked item
        ItemStack clicked = e.getCurrentItem();
        ItemMeta clicked_meta = clicked.getItemMeta();

        // Check if it was exit
        if(clicked_meta.getDisplayName().equalsIgnoreCase("exit") && e.getRawSlot() == 0) {
            // Remove the in progress sign
            signs.remove(cur);

            e.getWhoClicked().closeInventory();
            return;
        }

        // Make sure it's not removed when switching the UI
        can_remove = false;

        // Check if it was to proceed
        if(clicked_meta.getDisplayName().equalsIgnoreCase("next") && e.getRawSlot() == 8) {
            if(e.getView().getTitle().equalsIgnoreCase("Item Amount")) {
                player.openInventory(itemPrice);
            } else {
                player.sendMessage("Now right click on a chest to select it!");
                player.closeInventory();
            }
            return;
        }

        // Check if it is buying or selling
        else if(e.getView().getTitle().equalsIgnoreCase("Selling or Buying")) {
            // If it was selling
            if(clicked_meta.getDisplayName().equalsIgnoreCase("Buy") && e.getRawSlot() == 5) {
                cur.selling = false;
            }
            // If it was buying
            else if(clicked_meta.getDisplayName().equalsIgnoreCase("Sell") && e.getRawSlot() == 3) {
                cur.selling = true;
            }

            inv = clone_inv(itemAmount, "Item Amount");
            inv.setItem(4, cur.item);
            player.openInventory(inv);
        }

        // Check if it was an increase or decrease in amount
        else if(e.getView().getTitle().equalsIgnoreCase("Item Amount")) {
            // If it was green wool
            if(clicked_meta.getDisplayName().equalsIgnoreCase("More") && e.getRawSlot() == 5) {
                // Make sure were not at the max stack size
                if(cur.item.getAmount() == cur.item.getMaxStackSize()) {
                    return;
                }

                // Add one to the amount of the in progress sign
                cur.item.setAmount(cur.item.getAmount()+1);

                // Update the ui
                inv.setItem(4, cur.item);
                player.openInventory(inv);
                return;
            }

            // If it was red wool
            else if(clicked_meta.getDisplayName().equalsIgnoreCase("Less") && e.getRawSlot() == 3) {
                // Make sure were not at the stack minimum
                if(cur.item.getAmount() == 1) {
                    return;
                }

                // Subtract one from the amount of the in progress sign
                cur.item.setAmount(cur.item.getAmount()-1);

                // Update the ui
                inv.setItem(4, cur.item);
                player.openInventory(inv);
                return;
            }
        }

        // Check if we are changing the price
        else if(e.getView().getTitle().equalsIgnoreCase("Item Price")) {
            // If it was green wool
            if(clicked_meta.getDisplayName().equalsIgnoreCase("More") && e.getRawSlot() == 5) {
                // Make sure were not at the max stack size
                if(cur.price == 64) {
                    return;
                }

                // Add one to the amount of the in progress sign
                cur.price++;

                // Update the ui
                inv.setItem(4, new ItemStack(Material.GOLD_INGOT, cur.price));
                player.openInventory(inv);
                return;
            }

            // If it was red wool
            else if(clicked_meta.getDisplayName().equalsIgnoreCase("Less") && e.getRawSlot() == 3) {
                // Make sure were not at the stack minimum
                if(cur.price == 1) {
                    return;
                }

                // Subtract one from the amount of the in progress sign
                cur.price--;

                // Update the ui
                inv.setItem(4, new ItemStack(Material.GOLD_INGOT, cur.price));
                player.openInventory(inv);
                return;
            }
        }

        // Allow removal again
        can_remove = true;
    }
}
