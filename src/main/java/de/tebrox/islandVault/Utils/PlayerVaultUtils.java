package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.Events.VaultUpdateEvent;
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
        int toExtract = Math.min(vaultAmount, getAmount); // tatsächliche Entnahmemenge
        System.out.println(toExtract);

        VaultUpdateEvent event = new VaultUpdateEvent(player, ownerUUID, material, -toExtract, false);
        Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()) return null;

        inventory.put(material, vaultAmount - toExtract); // neue Menge speichern

        return new ItemStack(material, toExtract);
    }

}
