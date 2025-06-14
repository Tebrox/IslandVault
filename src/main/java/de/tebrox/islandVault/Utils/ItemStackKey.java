package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.Base64ItemSerializer;
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
        return new ItemStackKey(Base64ItemSerializer.serialize(stack));
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
        return Base64ItemSerializer.deserialize(key);
    }
}