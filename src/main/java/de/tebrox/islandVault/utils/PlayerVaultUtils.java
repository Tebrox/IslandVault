package de.tebrox.islandVault.utils;

import de.tebrox.islandVault.items.VaultItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerVaultUtils {

    private Player player;
    private List<VaultItem> inventory;
    private List<Material> unlockedMaterial = new ArrayList<>();

    public PlayerVaultUtils(Player player, List<VaultItem> inventory) {
        this.player = player;
        this.inventory = inventory;
    }

    public PlayerVaultUtils(Player player) {
        List<VaultItem> temp = new ArrayList<>();
        new PlayerVaultUtils(player, temp);
    }

    public UUID getOwnerUUID() {
        return player.getUniqueId();
    }

    public Player getPlayer() {
        return player;
    }

    public List<VaultItem> getInventory() {
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

}
