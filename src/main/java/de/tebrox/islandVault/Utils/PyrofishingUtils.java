package de.tebrox.islandVault.Utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PyrofishingUtils {

    //TODO
    public static boolean isPyroFishingItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<String> lore = meta.getLore();
        if (lore == null) return false;

        String[] keywords = {"Information", "Stufe", "Gewicht", "Preis", "Biome"};
        Set<String> found = new HashSet<>();

        for (String line : lore) {
            for (String key : keywords) {
                if (line.toLowerCase().contains(key.toLowerCase())) {
                    found.add(key);
                }
            }
        }

        // Mindestens 3 verschiedene Keywords müssen gefunden werden
        return found.size() >= 3;
    }
}
