package de.tebrox.islandVault.Utils;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtils {
    public static ItemStack removeAllEnchantments(ItemStack item) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
            for (Enchantment enchantment : storageMeta.getStoredEnchants().keySet()) {
                storageMeta.removeStoredEnchant(enchantment);
            }
            item.setItemMeta(storageMeta);
        } else {
            for (Enchantment enchantment : meta.getEnchants().keySet()) {
                meta.removeEnchant(enchantment);
            }
            item.setItemMeta(meta);
        }

        return item;
    }
}
