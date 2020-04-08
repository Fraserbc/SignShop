package net.earthmc.common;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class ItemStack_Serialiser {
    public ItemStack deserialise(String serialised) throws InvalidConfigurationException {
        // Load the string
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(serialised);

        ItemStack is = config.getItemStack("item");

        return is;
    }

    public String serialise(ItemStack is) throws InvalidConfigurationException {
        // Load the string
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", is);

        String serialised = config.saveToString();

        return serialised;
    }
}
