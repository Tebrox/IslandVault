package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.Base64ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class ItemStackKey {

    private final String key;

    private ItemStackKey(String key) {
        this.key = key;
    }

    public static ItemStackKey of(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return new ItemStackKey("AIR");
        ItemStack copy = stack.clone();
        copy.setAmount(1);

        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            meta.setLore(null);  // Lore entfernen
            copy.setItemMeta(meta);
        }

        return new ItemStackKey(Base64ItemSerializer.serializeItemStack(copy));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStackKey)) return false;
        ItemStackKey that = (ItemStackKey) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key;
    }

    public ItemStack toItemStack() {
        if ("AIR".equals(key)) return new ItemStack(Material.AIR);
        try {
            return Base64ItemSerializer.deserialize(key);
        } catch (Exception e) {
            PluginLogger.warning("[ItemStackKey] Fehler beim Deserialisieren: " + e.getMessage());
            return null; // oder: new ItemStack(Material.BARRIER)
        }
    }

    public static ItemStackKey fromString(String key) {
        return new ItemStackKey(key);
    }
}