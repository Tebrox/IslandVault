package de.tebrox.islandVault;

import de.tebrox.islandVault.Commands.VaultMainCommand;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Manager.ItemManager;
import de.tebrox.islandVault.Listeners.InventoryCloseListener;
import de.tebrox.islandVault.Listeners.JoinLeaveListener;
import de.tebrox.islandVault.Manager.VaultManager;
import de.tebrox.islandVault.Utils.PlayerVaultUtils;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public final class IslandVault extends JavaPlugin {

    private static IslandVault plugin;
    private static ItemManager itemManager;
    private static VaultManager vaultManager;
    FileConfiguration config = getConfig();
    private static HashMap<String, List<String>> permissionGroups = new HashMap<>();
    private static List<String> itemLore = new ArrayList<>();
    private static MainCommand mainCommand;

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

        registerCommandsAndEvents();

        PluginManager manager = getServer().getPluginManager();
        registerPermissions(manager);

        for(Player player : getServer().getOnlinePlayers()) {
            if(!IslandVault.getVaultManager().getVaults().containsKey(player.getUniqueId())) {
                IslandVault.getVaultManager().loadVault(player);
            }
        }

        itemLore = config.getStringList(Permissions.MESSAGE_ITEMLORE.getLabel());

    }

    @Override
    public void onDisable() {
        if(!getVaultManager().getVaults().isEmpty()) {
            for(Map.Entry<UUID, PlayerVaultUtils> entry : getVaultManager().getVaults().entrySet()) {
                IslandVault.getVaultManager().saveVault(entry.getValue());
                IslandVault.getVaultManager().getVaults().remove(entry.getKey());
            }
        }

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

    public static List<String> getItemLore() {
        return itemLore;
    }

    private void registerPermissions(PluginManager manager) {
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
        manager.addPermission(new Permission(Permissions.CAN_OPEN_MENU.getLabel()));
    }

    private void registerCommandsAndEvents() {
        mainCommand = new VaultMainCommand();
        mainCommand.registerMainCommand(this, "insellager");

        getServer().getPluginManager().registerEvents(new JoinLeaveListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);
    }

    public static MainCommand getMainCommand() {
        return mainCommand;
    }
}
