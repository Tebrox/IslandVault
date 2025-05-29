package de.tebrox.islandVault;

import de.tebrox.islandVault.Commands.VaultMainCommand;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Listeners.*;
import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Manager.ItemManager;
import de.tebrox.islandVault.Manager.LanguageManager;
import de.tebrox.islandVault.Manager.VaultManager;
import de.tebrox.islandVault.Utils.IslandUtils;
import de.tebrox.islandVault.Utils.PlayerVaultUtils;
import de.tebrox.islandVault.Utils.PluginDependencyChecker;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import javax.swing.plaf.basic.BasicButtonUI;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;

public final class IslandVault extends JavaPlugin {

    private static IslandVault plugin;
    private static ItemManager itemManager;
    private static VaultManager vaultManager;
    FileConfiguration config = getConfig();
    private static HashMap<String, List<String>> permissionGroups = new HashMap<>();
    private static MainCommand mainCommand;
    private Logger logger;
    private Map<String, Integer> radiusPermissionMap = new HashMap<>();
    private static LanguageManager languageManager;

    @Override
    public void onEnable() {
        setupLogger();
        plugin = this;

        if(!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        PluginDependencyChecker.setup(new String[]{"BentoBox", "Luckperms"}, new String[]{});
        PluginDependencyChecker.checkDependencies();


        if (!PluginDependencyChecker.allRequiredPresent()) {
            getVaultLogger().severe("Missing required plugins: " + PluginDependencyChecker.getFormattedList(PluginDependencyChecker.getMissingRequiredPlugins()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!PluginDependencyChecker.getMissingOptionalPlugins().isEmpty()) {
            getLogger().warning("Missing optional plugins: " + PluginDependencyChecker.getFormattedList(PluginDependencyChecker.getMissingOptionalPlugins()));
        }

        saveResource("config.yml", false);
        saveDefaultConfig();

        updateConfig();

        languageManager = new LanguageManager(this);

        MenuManager.setup(getServer(), this);

        itemManager = new ItemManager(this);
        vaultManager = new VaultManager(this);

        IslandUtils.setBentoBoxAvailable(getServer().getPluginManager().getPlugin("BentoBox") instanceof BentoBox);

        registerCommandsAndEvents();

        PluginManager manager = getServer().getPluginManager();
        registerPermissions(manager);

        loadAutoCollectPermissions(manager);

        for(Player player : getServer().getOnlinePlayers()) {
            System.out.println("Player: " + player.getName());
            System.out.println(player.getLocation());
            Island island = IslandUtils.getIslandManager().getIsland(player.getWorld(), player.getUniqueId());


            if(island == null) System.out.println("Island is null");

            if(island != null) {
                System.out.println("IslandOwner: " + Bukkit.getOfflinePlayer(island.getOwner()));
                UUID ownerUUID = island.getOwner();

                IslandVault.getVaultManager().loadVault(ownerUUID);
            }
        }

    }

    @Override
    public void onDisable() {
        if(!getVaultManager().getVaults().isEmpty()) {
            for(Map.Entry<UUID, PlayerVaultUtils> entry : getVaultManager().getVaults().entrySet()) {
                IslandVault.getVaultManager().saveVault(entry.getValue());
                System.out.println("Saving vault");
            }
            getVaultManager().getVaults().clear();
        }

    }

    public static IslandVault getPlugin() {
        return plugin;
    }

    public static ItemManager getItemManager() {
        return itemManager;
    }

    public static VaultManager getVaultManager() {
        return vaultManager;
    }

    public static HashMap<String, List<String>> getPermissionGroups() {
        return permissionGroups;
    }

    private void setupLogger() {

        logger = Logger.getLogger("IslandVault");
        logger.setUseParentHandlers(false);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new java.util.logging.Formatter() {
            private final String RESET = "\u001B[0m";
            private final String GREEN = "\u001B[32m";

            @Override
            public String format(LogRecord record) {
                String prefix = GREEN + "[IslandVault] " + RESET;
                if(record.getLevel() != Level.INFO) {
                    prefix += record.getLevel() + ": ";
                }

                return prefix + record.getMessage() + "\n";
            }
        });

        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
    }

    private void registerPermissions(PluginManager manager) {
        ConfigurationSection section = config.getConfigurationSection(Permissions.GROUPS_CONFIG.getLabel());

        for(String groupLabel : section.getKeys(false)) {
            manager.addPermission(new Permission(Permissions.GROUPS.getLabel() + groupLabel));
            if(!permissionGroups.containsKey(groupLabel)) {
                List<String> permissions = getConfig().getStringList(Permissions.GROUPS_CONFIG.getLabel() + "." + groupLabel);
                permissionGroups.put(groupLabel, permissions);
                getVaultLogger().log(Level.INFO, "Loaded group: " + groupLabel + " with entries " + permissions.toString());
            }
        }

        for(Material material : itemManager.getMaterialList()) {
            manager.addPermission(new Permission(Permissions.VAULT.getLabel() + material.toString().toLowerCase()));
        }
        manager.addPermission(new Permission(Permissions.COMMAND.getLabel() + "openGUI"));
    }

    private void registerCommandsAndEvents() {
        mainCommand = new VaultMainCommand();
        mainCommand.registerMainCommand(this, "insellager");

        getServer().getPluginManager().registerEvents(new OPJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);
        getServer().getPluginManager().registerEvents(new ItemAutoCollectListener(this), this);
        getServer().getPluginManager().registerEvents(new IslandListener(), this);
    }

    public void loadAutoCollectPermissions(PluginManager manager) {
        radiusPermissionMap.clear();

        List<Integer> radii = getConfig().getIntegerList("auto-collect-radius");
        for (int i = 0; i < radii.size(); i++) {
            int radius = radii.get(i);
            if (radius > 0) {
                String permission = Permissions.COLLECT_RADIUS.getLabel() + radius;
                manager.addPermission(new Permission(permission));
                radiusPermissionMap.put(permission, radius);
            }
        }
    }

    public void updateConfig() {
        FileConfiguration config = getConfig();

        // 3. Default-Config aus Resource laden (ohne zu speichern)
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(getResource("config.yml"), StandardCharsets.UTF_8)
        );

        // 4. Setze die Default-Config als Defaultwerte
        config.setDefaults(defaultConfig);
        config.options().copyDefaults(true);

        saveConfig();
    }

    public static MainCommand getMainCommand() {
        return mainCommand;
    }

    public Logger getVaultLogger() {
        return logger;
    }

    public Map<String, Integer> getRadiusPermissionMap() {
        return radiusPermissionMap;
    }

    public static LanguageManager getLanguageManager() {
        return languageManager;
    }
}
