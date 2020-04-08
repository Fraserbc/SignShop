package net.earthmc.common;

import org.bukkit.Material;

public class SignEnum {
    public String enumToSign(String name) {
        String[] split = name.split("_");

        String out = "";
        for (String sub : split) {
            out += sub.substring(0, 1).toUpperCase() + sub.substring(1).toLowerCase();
        }

        return out;
    }

    public Material signToEnum(String sign) {
        String[] split = sign.split("(?=\\p{Upper})");

        for(int i = 0; i < split.length; i++) {
            split[i] = split[i].toUpperCase();
        }

        return Material.valueOf(String.join("_", split));
    }
}
