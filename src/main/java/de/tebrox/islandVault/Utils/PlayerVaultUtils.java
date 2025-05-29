package de.tebrox.islandVault.Utils;

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

    public void addUnlockedMaterial(Material material) {
        if(!unlockedMaterial.contains(material)) {
            unlockedMaterial.add(material);
        }
    }

    public List<Material> getUnlockedMaterial() {
        return unlockedMaterial;
    }

    public ItemStack setItem(Material material, int vaultAmount, int getAmount) {
        if(vaultAmount >= getAmount) {
            inventory.put(material, vaultAmount - getAmount);
            return new ItemStack(material, getAmount);
        }
        inventory.put(material, 0);
        return new ItemStack(material, vaultAmount);
    }

}
