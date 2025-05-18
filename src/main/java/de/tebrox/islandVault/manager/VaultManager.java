package de.tebrox.islandVault.manager;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.enums.Permissions;
import de.tebrox.islandVault.items.VaultItem;
import de.tebrox.islandVault.menu.VaultMenu;
import de.tebrox.islandVault.utils.PlayerVaultUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VaultManager {
    public HashMap<UUID, PlayerVaultUtils> vaults = new HashMap<>();
    private final IslandVault plugin;

    public VaultManager(IslandVault plugin) {
        this.plugin = plugin;
    }

    public void loadVault(Player player) {
        File folder = new File(plugin.getDataFolder() + File.separator + "vaults");

        if(!folder.exists()) {
            folder.mkdirs();
        }

        if(!vaults.containsKey(player.getUniqueId())) {

            File dataFile = new File(plugin.getDataFolder() + File.separator + "vaults" + File.separator + player.getUniqueId() + ".yml");
            List<VaultItem> vaultItemList = new ArrayList<>();

            if(!dataFile.exists()) {

                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                data.set("Player.name", player.getName());
                for(Material material : IslandVault.getItemManager().getMaterialList()) {
                    data.set("Inventory." + material, 0);
                    vaultItemList.add(new VaultItem(material));
                }
                try {
                    data.save(dataFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                PlayerVaultUtils utils = new PlayerVaultUtils(player, vaultItemList);
                vaults.put(player.getUniqueId(), utils);

                return;
            }
            FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection section = data.getConfigurationSection("Inventory");

            for(String material : section.getKeys(false)) {
                Material mat = Material.getMaterial(material);
                if(!IslandVault.getItemManager().materialIsBlacklisted(mat)) {
                    int amount = data.getInt("Inventory." + material);
                    vaultItemList.add(new VaultItem(mat, amount));
                    System.out.println(material + ": " + amount);
                }
            }

            for(VaultItem vaultItem : vaultItemList) {
                System.out.println("VaultItem (" + vaultItem.getMaterial() + "): " + vaultItem.getAmount());
            }

            PlayerVaultUtils utils = new PlayerVaultUtils(player, vaultItemList);
            vaults.put(player.getUniqueId(), utils);
        }
    }

    public void saveVault(PlayerVaultUtils playerVaultUtils) {
        File folder = new File(plugin.getDataFolder() + File.separator + "vaults");

        if(!folder.exists()) {
            folder.mkdirs();
        }

        File dataFile = new File(plugin.getDataFolder() + File.separator + "vaults" + File.separator + playerVaultUtils.getPlayer().getUniqueId() + ".yml");

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("Player.name", playerVaultUtils.getPlayer().getName());

        for(VaultItem vaultItem : playerVaultUtils.getInventory()) {
            if(playerVaultUtils.getUnlockedMaterial().contains(vaultItem.getMaterial())) {
                data.set("Inventory." + vaultItem.getMaterial(), vaultItem.getAmount());
            }
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public List<ItemStack> getPlayerVaultItems(UUID uuid) {

        if(!getVaults().containsKey(uuid)) {
            System.out.println("No Items at inventory from " + Bukkit.getPlayer(uuid));
            return null;
        }

        PlayerVaultUtils playerVaultUtils = getVaults().get(uuid);
        Player player = playerVaultUtils.getPlayer();

        List<Material> materialList = IslandVault.getItemManager().getMaterialList();
        HashMap<String, List<String>> permissionGroups = IslandVault.getPermissionGroups();

        for(Map.Entry<String, List<String>> entry : permissionGroups.entrySet()) {
            if(player.hasPermission(Permissions.GROUPS.getLabel() + entry.getKey())) {
                for(String materialString : entry.getValue()) {
                    Material material = Material.getMaterial(materialString);
                    if(material != null) {
                        playerVaultUtils.addUnlockedMaterial(material);
                    }
                }
            }
        }

        for(Material material : materialList) {
            if(!playerVaultUtils.getUnlockedMaterial().contains(material) && player.hasPermission(Permissions.VAULT.getLabel() + material.toString().toLowerCase())) {
                playerVaultUtils.addUnlockedMaterial(material);
            }
        }

        List<ItemStack> items = new ArrayList<>();
        List<VaultItem> vaultItems = playerVaultUtils.getInventory();
        List<Material> unlockedMaterials = playerVaultUtils.getUnlockedMaterial();

        for(VaultItem vaultItem : vaultItems) {
            if(unlockedMaterials.contains(vaultItem.getMaterial())) {
                items.add(vaultItem.getItemStack());
            }
        }


        return items; //TODO save and load from json file
    }

    public HashMap<UUID, PlayerVaultUtils> getVaults() {
        return vaults;
    }

}
