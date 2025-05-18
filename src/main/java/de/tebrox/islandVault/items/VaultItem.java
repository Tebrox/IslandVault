package de.tebrox.islandVault.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class VaultItem {
    private Material material;
    private int amount;

    public VaultItem(Material material) {
        new VaultItem(material, 0);
    }

    public VaultItem(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Material getMaterial() {
        return material;
    }

    public ItemStack getItemStack() {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        lore.add("Amount: " + getAmount());

        meta.setLore(lore);

        item.setItemMeta(meta);

        return item;
    }

}
