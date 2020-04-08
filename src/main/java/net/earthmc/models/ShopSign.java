package net.earthmc.models;

import org.bukkit.inventory.ItemStack;

public class ShopSign {
    public double timeout;
    public location sign;
    public location chest;
    public String owner;
    public ItemStack item;
    public int price;
    public boolean done;
    public boolean selling;

    public ShopSign(double timeout, location sign, location chest, String owner, ItemStack item, int price, boolean selling) {
        this.timeout = timeout;
        this.sign = sign;
        this.chest = chest;
        this.owner = owner;
        this.item = item;
        this.price = price;
        done = false;
        this.selling = selling;
    }
}
