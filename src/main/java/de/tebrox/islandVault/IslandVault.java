package de.tebrox.islandVault;

import de.tebrox.islandVault.commands.OpenCommand;
import de.tebrox.islandVault.enums.Permissions;
import de.tebrox.islandVault.items.ItemManager;
import de.tebrox.islandVault.items.VaultItem;
import de.tebrox.islandVault.listeners.InventoryCloseListener;
import de.tebrox.islandVault.listeners.JoinLeaveListener;
import de.tebrox.islandVault.manager.VaultManager;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public final class IslandVault extends JavaPlugin {

    private static IslandVault plugin;
    private static ItemManager itemManager;
    private static VaultManager vaultManager;
    FileConfiguration config = getConfig();
    private static HashMap<String, List<String>> permissionGroups = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;

        if(!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        saveResource("config.yml", false);
        saveDefaultConfig();

        MenuManager.setup(getServer(), this);

        itemManager = new ItemManager(this);
        vaultManager = new VaultManager(this);

        this.getCommand("islandvault").setExecutor(new OpenCommand());

        PluginManager manager = getServer().getPluginManager();

        ConfigurationSection section = config.getConfigurationSection(Permissions.GROUPS_CONFIG.getLabel());

        for(String groupLabel : section.getKeys(false)) {
            manager.addPermission(new Permission(Permissions.GROUPS.getLabel() + groupLabel));
            if(!permissionGroups.containsKey(groupLabel)) {
                List<String> permissions = getConfig().getStringList(Permissions.GROUPS_CONFIG.getLabel() + "." + groupLabel);
                permissionGroups.put(groupLabel, permissions);
                getLogger().log(Level.INFO, "Loaded group: " + groupLabel + " with entries: " + permissions.toString());
            }
        }

        for(Material material : itemManager.getMaterialList()) {
            manager.addPermission(new Permission(Permissions.VAULT.getLabel() + material.toString().toLowerCase()));
        }

        getServer().getPluginManager().registerEvents(new JoinLeaveListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static IslandVault getPlugin() {
        return plugin;
    }

    public static ItemManager getItemManager() {
        return itemManager;
    }

    public static  VaultManager getVaultManager() {
        return vaultManager;
    }

    public static HashMap<String, List<String>> getPermissionGroups() {
        return permissionGroups;
    }

}
