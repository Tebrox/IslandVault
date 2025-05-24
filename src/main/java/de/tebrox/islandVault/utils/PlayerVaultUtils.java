package de.tebrox.islandVault.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerVaultUtils {

    private Player player;
    private HashMap<Material, Integer> inventory;
    private List<Material> unlockedMaterial = new ArrayList<>();

    public PlayerVaultUtils(Player player, HashMap<Material, Integer> inventory) {
        this.player = player;
        this.inventory = inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public HashMap<Material, Integer> getInventory() {
        return inventory;
    }

    public void addUnlockedMaterial(Material material) {
        if(!unlockedMaterial.contains(material)) {
            unlockedMaterial.add(material);
        }
    }

    public List<Material> getUnlockedMaterial() {
        return unlockedMaterial;
    }

    public ItemStack setItem(Material material, int amount, int difference) {
        if(amount > 0) {
            inventory.put(material, amount - difference);
        }else{
            inventory.put(material, 0);
        }

        return new ItemStack(material, difference);
    }

}
