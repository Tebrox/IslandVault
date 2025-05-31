package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.Events.ItemRemovedFromVaultEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerVaultUtils {

    private UUID ownerUUID;
    private HashMap<Material, Integer> inventory;
    private List<Material> unlockedMaterial = new ArrayList<>();
    private boolean autoCollect = false;

    public PlayerVaultUtils(UUID ownerUUID, HashMap<Material, Integer> inventory) {
        this.inventory = inventory;
        this.ownerUUID = ownerUUID;
    }

    public HashMap<Material, Integer> getInventory() {
        return inventory;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public boolean getAutoCollect() {
        return autoCollect;
    }

    public void setAutoCollect(boolean value) {
        this.autoCollect = value;
    }

    public void addUnlockedMaterial(Material material) {
        if(!unlockedMaterial.contains(material)) {
            unlockedMaterial.add(material);
        }
    }

    public List<Material> getUnlockedMaterial() {
        return unlockedMaterial;
    }

    public ItemStack setItem(Material material, int vaultAmount, int getAmount, Player player) {
        if(vaultAmount >= getAmount) {

            ItemStack tempItemStack = new ItemStack(material, getAmount);
            ItemRemovedFromVaultEvent event = new ItemRemovedFromVaultEvent(player, ownerUUID, tempItemStack, false);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return null;

            inventory.put(material, vaultAmount - getAmount);

            return tempItemStack;
        }
        ItemStack tempItemStack = new ItemStack(material, vaultAmount);
        ItemRemovedFromVaultEvent event = new ItemRemovedFromVaultEvent(player, ownerUUID, tempItemStack, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        inventory.put(material, 0);
        return tempItemStack;
    }

}
