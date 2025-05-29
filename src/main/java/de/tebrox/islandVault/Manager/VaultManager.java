package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import de.tebrox.islandVault.Utils.PlayerVaultUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VaultManager {
    public HashMap<UUID, PlayerVaultUtils> vaults = new HashMap<>();
    private final IslandVault plugin;

    public VaultManager(IslandVault plugin) {
        this.plugin = plugin;
    }


    /**
     * Loads the vault from the file
     * @param ownerUUID
     */
    public void loadVault(UUID ownerUUID) {
        File folder = new File(plugin.getDataFolder() + File.separator + "vaults");

        if(!folder.exists()) {
            folder.mkdirs();
        }

        if(!vaults.containsKey(ownerUUID)) {

            File dataFile = new File(plugin.getDataFolder() + File.separator + "vaults" + File.separator + ownerUUID + ".yml");
            HashMap<Material, Integer> vaultItemList = new HashMap<>();

            if(!dataFile.exists()) {

                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                data.set("Player.name", Bukkit.getOfflinePlayer(ownerUUID).getName());
                for(Material material : IslandVault.getItemManager().getMaterialList()) {
                    data.set("Inventory." + material, 0);
                    vaultItemList.put(material, 0);
                }
                try {
                    data.save(dataFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                PlayerVaultUtils utils = new PlayerVaultUtils(ownerUUID, vaultItemList);
                vaults.put(ownerUUID, utils);

                return;
            }
            FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection section = data.getConfigurationSection("Inventory");

            if(section == null) return;

            for(String material : section.getKeys(false)) {
                Material mat = Material.getMaterial(material);
                if(!IslandVault.getItemManager().materialIsBlacklisted(mat)) {
                    int amount = data.getInt("Inventory." + material);
                    vaultItemList.put(mat, amount);
                }
            }

            PlayerVaultUtils utils = new PlayerVaultUtils(ownerUUID, vaultItemList);
            vaults.put(ownerUUID, utils);
        }
    }

    /**
     * Saves the vault to the filestorage
     * @param playerVaultUtils
     */
    public void saveVault(PlayerVaultUtils playerVaultUtils) {
        File folder = new File(plugin.getDataFolder() + File.separator + "vaults");

        if(!folder.exists()) {
            folder.mkdirs();
        }

        File dataFile = new File(plugin.getDataFolder() + File.separator + "vaults" + File.separator + playerVaultUtils.getOwnerUUID() + ".yml");

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("Player.name", Bukkit.getOfflinePlayer(playerVaultUtils.getOwnerUUID()).getName());

        for(Map.Entry<Material, Integer> entry : playerVaultUtils.getInventory().entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            if(playerVaultUtils.getUnlockedMaterial().contains(material)) {
                data.set("Inventory." + material, amount);
            }
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Get the list of itemstacks for the specific playervault
     * @param uuid the players uuid
     * @return List of itemstacks
     */
    public List<ItemStack> getPlayerVaultItems(Player player, UUID ownerUUID) {
        if(!getVaults().containsKey(ownerUUID)) {
            return new ArrayList<>();
        }

        PlayerVaultUtils playerVaultUtils = getVaults().get(ownerUUID);

        List<Material> materialList = IslandVault.getItemManager().getMaterialList();
        HashMap<String, List<String>> permissionGroups = IslandVault.getPermissionGroups();

        for(Map.Entry<String, List<String>> entry : permissionGroups.entrySet()) {
            if(LuckPermsUtils.hasPermissionForGroup(ownerUUID, entry.getKey())) {
                for(String materialString : entry.getValue()) {
                    Material material = Material.getMaterial(materialString);
                    if(material != null) {
                        playerVaultUtils.addUnlockedMaterial(material);
                    }
                }
            }
        }

        for(Material material : materialList) {
            if(!playerVaultUtils.getUnlockedMaterial().contains(material) && LuckPermsUtils.hasPermissionForItem(ownerUUID, material)) {
                playerVaultUtils.addUnlockedMaterial(material);
            }
        }

        List<ItemStack> items = new ArrayList<>();
        HashMap<Material, Integer> vaultItems = playerVaultUtils.getInventory();
        List<Material> unlockedMaterials = playerVaultUtils.getUnlockedMaterial();

        for(Map.Entry<Material, Integer> entry : vaultItems.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            if(unlockedMaterials.contains(material)) {
                items.add(getItemStack(player, material, amount, player.isOp()));
            }
        }

        return items;
    }

    public HashMap<UUID, PlayerVaultUtils> getVaults() {
        return vaults;
    }

    public boolean addItemToVault(Material material, int amount, UUID ownerUUID) {
        if(getVaults().containsKey(ownerUUID)) {
            PlayerVaultUtils playerVaultUtils = getVaults().get(ownerUUID);

            if(playerVaultUtils.getUnlockedMaterial().contains(material)) {
                playerVaultUtils.getInventory().compute(material, (k, oldAmount) -> oldAmount + amount);
                return true;
            }
        }
        return false;
    }

    public ItemStack getItemFromVault(Material material, int amount, Player player) {
        if(getVaults().containsKey(player.getUniqueId())) {
            PlayerVaultUtils playerVaultUtils = getVaults().get(player.getUniqueId());

            if(playerVaultUtils.getUnlockedMaterial().contains(material)) {
                for(Map.Entry<Material, Integer> entry : playerVaultUtils.getInventory().entrySet()) {
                    Material key = entry.getKey();
                    int value = entry.getValue();
                    if(key.equals(material)) {
                        if(value == 0) {
                            return null;
                        }

                        return playerVaultUtils.setItem(material, value, amount);
                    }

                }
            }
        }
        return null;
    }

    public ItemStack getItemStack(Player player, Material material, int amount, boolean playerIsOp) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setLore(IslandVault.getLanguageManager().translateList(player, "menu.item.vaultItem.itemLore", Map.of("amount", String.valueOf(amount), "maxStackSize", String.valueOf(material.getMaxStackSize()))));

        if(playerIsOp) {
            List<String> lore = meta.getLore();
            String loreString = "&9Material: &f" + material;
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', loreString));
            meta.setLore(lore);
        }

        item.setItemMeta(meta);

        return item;
    }


}
