package de.tebrox.islandVault;

import de.tebrox.islandVault.Commands.AdminCommand.VaultAdminMainCommand;
import de.tebrox.islandVault.Commands.VaultMainCommand;
import de.tebrox.islandVault.Converter.ItemGroupConfigConverter;
import de.tebrox.islandVault.Converter.VaultMigrationManager;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Items.VaultChest;
import de.tebrox.islandVault.Items.VaultChestItem;
import de.tebrox.islandVault.Listeners.*;
import de.tebrox.islandVault.Manager.*;
import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Utils.*;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.eclipse.jdt.annotation.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

public final class IslandVault extends JavaPlugin {

    private static IslandVault plugin;
    private static ConfigManager configManager;
    private static ItemManager itemManager;
    private static VaultManager vaultManager;
    private static VaultChestManager vaultChestManager;
    FileConfiguration config = getConfig();
    private static MainCommand mainCommand;
    private static MainCommand adminMainCommand;
    //private Logger logger;
    private Map<String, Integer> radiusPermissionMap = new HashMap<>();
    private static LanguageManager languageManager;
    private boolean debug;


    @Override
    public void onEnable() {
        setupLogger();
        plugin = this;

        if(!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        configManager = new ConfigManager(plugin);
        configManager.loadConfig("config");
        //saveDefaultConfig(); // stellt sicher, dass config.yml im JAR vorhanden ist

        //reloadConfig();

        PluginDependencyChecker.setup(new String[]{"BentoBox", "Luckperms"}, new String[]{});
        PluginDependencyChecker.checkDependencies();


        if (!PluginDependencyChecker.allRequiredPresent()) {
            getLogger().severe("Missing required plugins: " + PluginDependencyChecker.getFormattedList(PluginDependencyChecker.getMissingRequiredPlugins()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!PluginDependencyChecker.getMissingOptionalPlugins().isEmpty()) {
            getLogger().warning("Missing optional plugins: " + PluginDependencyChecker.getFormattedList(PluginDependencyChecker.getMissingOptionalPlugins()));
        }

        MenuManager.setup(getServer(), this);

        languageManager = new LanguageManager(this);

        itemManager = new ItemManager(this);

        new VaultMigrationManager(this).migratePlayerVaultsToIslandVaults();

        vaultManager = new VaultManager(this);

        reloadPluginConfig(null);



        vaultChestManager = new VaultChestManager(this);
        vaultChestManager.loadAllChests();

        registerCommandsAndEvents();

        loadAutoCollectPermissions();

        VaultChestItem.registerRecipes(this);
        getServer().getPluginManager().registerEvents(new VaultChestRecipeListener(), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                LuckPerms lp = LuckPermsProvider.get();
                IslandsManager islandsManager = IslandUtils.getIslandManager();
                for(Player player : getServer().getOnlinePlayers()) {
                    if(islandsManager == null) {
                        //debugLogger.warning("Cannot load onlineplayer islands. Islandmanager is null!");
                    }

                    Island island = islandsManager.getIsland(player.getWorld(), player.getUniqueId());
                    if(island != null) {
                        UUID ownerUUID = island.getOwner();

                        IslandVault.getVaultManager().loadVault(ownerUUID);
                        if (lp.getUserManager().getUser(ownerUUID) == null) {
                            lp.getUserManager().loadUser(ownerUUID);
                        }
                    }
                }
            }
        }.runTaskLater(this, 40L);

        // 2. Owner alle 60 Sekunden "touchen"
        new BukkitRunnable() {
            @Override
            public void run() {
                LuckPerms lp = LuckPermsProvider.get();
                IslandsManager islandsManager = IslandUtils.getIslandManager();
                for (Player player : getServer().getOnlinePlayers()) {
                    if (islandsManager == null) {
                        continue;
                    }

                    Island island = islandsManager.getIsland(player.getWorld(), player.getUniqueId());
                    if (island != null) {
                        UUID ownerUUID = island.getOwner();

                        if (lp.getUserManager().getUser(ownerUUID) == null) {
                            lp.getUserManager().loadUser(ownerUUID);
                            //System.out.println("Load user in luckperms cache: " + Bukkit.getOfflinePlayer(ownerUUID).getName());
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 20L * 60, 20L * 60); // alle 60s
    }

    @Override
    public void onDisable() {
        if(!getVaultManager().getVaults().isEmpty()) {
            for(Map.Entry<UUID, PlayerVaultUtils> entry : getVaultManager().getVaults().entrySet()) {
                IslandVault.getVaultManager().saveVault(entry.getValue());
            }
            getVaultManager().getVaults().clear();
        }

        vaultChestManager.flushAndStopAsyncSave();
        configManager.flushAllAutosaves();
        configManager.clearChangeTracking();
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

    public static VaultChestManager getVaultChestManager() { return vaultChestManager; }

    private void setupLogger() {

        Logger logger = getLogger(); // Verwende den Bukkit-Logger

        // Alle bestehenden Handler durchgehen und Formatter setzen
        for (Handler handler : logger.getHandlers()) {
            handler.setFormatter(new Formatter() {
                private final String RESET = "\u001B[0m";
                private final String GREEN = "\u001B[32m";

                @Override
                public String format(LogRecord record) {
                    return record.getMessage() + "\n";
                }
            });
        }
    }

    private void registerPermissions() {
        ConfigurationSection section = config.getConfigurationSection(Permissions.GROUPS_CONFIG.getLabel());
        ItemGroupConfigConverter.convertOldGroups(this, configManager);
        ItemGroupManager.init(plugin, configManager);

        for(ItemStack stack : itemManager.getItemList()) {
            String permission = itemManager.buildPermissionKey(stack);
            if(PermissionUtils.registerPermission(permission, "Vault item " + stack.toString(), PermissionDefault.FALSE)) {
                //debugLogger.info("Registered vault item: " + material.toString());
            }else{
                //debugLogger.warning("Cannot register vault item: " + material.toString());
            }
        }
    }

    private void registerCommandsAndEvents() {
        mainCommand = new VaultMainCommand();
        mainCommand.registerMainCommand(this, "insellager");

        adminMainCommand = new VaultAdminMainCommand();
        adminMainCommand.registerMainCommand(this, "insellageradmin");

        AdminVaultLogger adminVaultLogger = new AdminVaultLogger(this);

        //getServer().getPluginManager().registerEvents(new OPJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemPermissionCache(this, LuckPermsProvider.get(), itemManager), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);
        getServer().getPluginManager().registerEvents(new ItemAutoCollectListener(this), this);
        getServer().getPluginManager().registerEvents(new IslandListener(), this);
        getServer().getPluginManager().registerEvents(new VaultEventListener(adminVaultLogger), this);
        getServer().getPluginManager().registerEvents(new ChunkLoadListener(this), this);

        //getServer().getPluginManager().registerEvents(new WorldLoadListener(this), this);
    }

    public void loadAutoCollectPermissions() {
        radiusPermissionMap.clear();

        List<Integer> radii = getConfig().getIntegerList("auto-collect-radius");
        for (int i = 0; i < radii.size(); i++) {
            int radius = radii.get(i);
            if (radius > 0) {
                String permission = Permissions.COLLECT_RADIUS.getLabel() + radius;
                if(PermissionUtils.registerPermission(permission, "Autocollect radius " + radius, PermissionDefault.FALSE)) {
                    //debugLogger.info("Registered autocolllect radius " + radius);
                }else{
                    //debugLogger.warning("Cannot register autocollect radius " + radius);
                }
                radiusPermissionMap.put(permission, radius);
            }
        }
    }

    public static MainCommand getMainCommand() {
        return mainCommand;
    }

    public static MainCommand getAdminMainCommand() {
        return adminMainCommand;
    }

    public Map<String, Integer> getRadiusPermissionMap() {
        return radiusPermissionMap;
    }

    public static LanguageManager getLanguageManager() {
        return languageManager;
    }

    public static ConfigManager getConfigManager() { return configManager; }

    public void reloadPluginConfig(CommandSender sender) {
        reloadConfig();
        debug = getConfig().getBoolean("debug", false);
        getLogger().info("Config neu geladen. Debugmodus ist " + (debug ? "aktiviert" : "deaktiviert"));
        if(sender != null) {
            sender.sendMessage("Config neu geladen. Debugmodus ist " + (debug ? "aktiviert" : "deaktiviert"));
        }
        getItemManager().reload();
        registerPermissions();

    }

    /**
     * @return debugmode state
     */
    public boolean isDebug() {
        return debug;
    }

    public void setDebugMode(boolean debug) {
        this.debug = debug;
        getConfig().set("debug", debug);
        saveConfig();
    }
}
