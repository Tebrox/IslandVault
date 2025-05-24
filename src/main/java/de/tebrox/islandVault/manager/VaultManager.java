package de.tebrox.islandVault.manager;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.enums.Permissions;
import de.tebrox.islandVault.utils.PlayerVaultUtils;
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
     * @param player
     */
    public void loadVault(Player player) {
        File folder = new File(plugin.getDataFolder() + File.separator + "vaults");

        if(!folder.exists()) {
            folder.mkdirs();
        }

        if(!vaults.containsKey(player.getUniqueId())) {

            File dataFile = new File(plugin.getDataFolder() + File.separator + "vaults" + File.separator + player.getUniqueId() + ".yml");
            HashMap<Material, Integer> vaultItemList = new HashMap<>();

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
                    vaultItemList.put(material, 0);
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
                    vaultItemList.put(mat, amount);
                }
            }

            PlayerVaultUtils utils = new PlayerVaultUtils(player, vaultItemList);
            vaults.put(player.getUniqueId(), utils);
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

        File dataFile = new File(plugin.getDataFolder() + File.separator + "vaults" + File.separator + playerVaultUtils.getPlayer().getUniqueId() + ".yml");

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("Player.name", playerVaultUtils.getPlayer().getName());

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
    public List<ItemStack> getPlayerVaultItems(UUID uuid) {

        if(!getVaults().containsKey(uuid)) {
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
        HashMap<Material, Integer> vaultItems = playerVaultUtils.getInventory();
        List<Material> unlockedMaterials = playerVaultUtils.getUnlockedMaterial();

        for(Map.Entry<Material, Integer> entry : vaultItems.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            if(unlockedMaterials.contains(material)) {
                items.add(getItemStack(material, amount));
            }
        }

        return items;
    }

    public HashMap<UUID, PlayerVaultUtils> getVaults() {
        return vaults;
    }

    public boolean addItemToVault(Material material, int amount, Player player) {
        if(getVaults().containsKey(player.getUniqueId())) {
            PlayerVaultUtils playerVaultUtils = getVaults().get(player.getUniqueId());

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
                ItemStack returnItem = null;
                for(Map.Entry<Material, Integer> entry : playerVaultUtils.getInventory().entrySet()) {
                    Material key = entry.getKey();
                    int value = entry.getValue();
                    if(key.equals(material)) {
                        if(value == 0) {
                            return null;
                        }

                        if(value > amount) {
                            returnItem = playerVaultUtils.setItem(material, value, amount);
                        }else{
                            returnItem = playerVaultUtils.setItem(material, 0, amount);
                        }
                        return returnItem;
                    }

                }
            }
        }
        return null;
    }

    public ItemStack getItemStack(Material material, int amount) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.BLUE + "Menge: " + amount);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Linksklick: " + material.getMaxStackSize() + "x");
        lore.add(ChatColor.YELLOW + "Rechtsklick: 1x");
        lore.add(ChatColor.YELLOW + "Shiftklick: direkt ins Inventar");

        meta.setLore(lore);

        item.setItemMeta(meta);

        return item;
    }
}
