package de.tebrox.islandVault;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.tebrox.islandVault.Commands.AdminCommand.VaultAdminMainCommand;
import de.tebrox.islandVault.Commands.VaultMainCommand;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Listeners.*;
import de.tebrox.islandVault.Manager.*;
import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Utils.*;
import de.tebrox.islandvault.api.*;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

public final class IslandVault extends JavaPlugin {

    //Main class
    private static IslandVault plugin;

    //Manager classes
    private static ParticleManager particleManager;
    private static RegionManager regionManager;
    private static VaultManager vaultManager;
    private static VaultSyncManager vaultSyncManager;
    private static LanguageManager languageManager;

    //Permission
    private static PermissionItemRegistry permissionItemRegistry;
    private static HashMap<String, List<String>> permissionGroups = new HashMap<>();
    private Map<String, Integer> radiusPermissionMap = new HashMap<>();

    //Commands
    private static MainCommand mainCommand;
    private static MainCommand adminMainCommand;

    private boolean debug;
    private boolean firstStart = false;

    @Override
    public void onEnable() {
        setupLogger();
        plugin = this;

        if(!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        saveDefaultConfig(); // stellt sicher, dass config.yml im JAR vorhanden ist

        File configFile = new File(getDataFolder(), "config.yml");
        ConfigUpdater updater = new ConfigUpdater(configFile);

        InputStream defaultConfigStream = getResource("config.yml");
        if (defaultConfigStream == null) {
            getLogger().severe("Default config.yml konnte nicht gefunden werden!");
            return;
        }

        try {
            updater.update(defaultConfigStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        reloadConfig();

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

        BentoBoxRanks.loadRanks();
        loadAddons();

        MenuManager.setup(getServer(), this);

        languageManager = new LanguageManager(this);
        particleManager = new ParticleManager(this);

        vaultManager = new VaultManager(this);
        vaultSyncManager = new VaultSyncManager(this);
        permissionItemRegistry = new PermissionItemRegistry(this);
        regionManager = new RegionManager(this);
        ItemGroupManager.init(this);

        reloadPluginConfig(null);

        registerCommandsAndEvents();

        registerPermissions();

        loadAutoCollectPermissions();

        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : getServer().getOnlinePlayers()) {
                    IslandsManager islandsManager = IslandUtils.getIslandManager();

                    Island island = islandsManager.getIsland(player.getWorld(), player.getUniqueId());
                    if (island != null) {
                        IslandTracker.setPlayerIsland(player.getUniqueId(), island);
                        IslandVault.getVaultManager().loadOrCreateVault(island.getUniqueId(), island.getOwner());
                    } else {
                        IslandTracker.removePlayerIsland(player.getUniqueId());
                    }
                }
            }
        }.runTaskLater(this, 40L);
    }

    @Override
    public void onDisable() {
        vaultManager.flushAll();
        particleManager.stopAll();
    }

    public static IslandVault getPlugin() {
        return plugin;
    }

    public static VaultManager getVaultManager() { return vaultManager; }
    public static RegionManager getRegionManager() { return  regionManager; }
    public static ParticleManager getParticleManager() { return particleManager; }
    public static VaultSyncManager getVaultSyncManager() { return vaultSyncManager; }

    public static PermissionItemRegistry getPermissionItemRegistry() { return permissionItemRegistry; }
    public static HashMap<String, List<String>> getPermissionGroups() { return permissionGroups; }

    private void setupLogger() {

        Logger logger = getLogger(); // Verwende den Bukkit-Logger

        // Alle bestehenden Handler durchgehen und Formatter setzen
        for (Handler handler : logger.getHandlers()) {
            handler.setFormatter(new Formatter() {
                private final String RESET = "\u001B[0m";
                private final String GREEN = "\u001B[32m";

                @Override
                public String format(LogRecord record) {
                    String prefix = "[IslandVault] ";

                    // Optional: Level anzeigen, aber nur wenn nicht INFO
                    if (record.getLevel() != Level.INFO) {
                        prefix += record.getLevel().getName() + ": ";
                    }

                    return prefix + record.getMessage() + "\n";
                }
            });
        }
        getLogger().setParent(logger);
    }

    private void loadAddons() {
        for (AddonProvider provider : AddonRegistry.getAllProviders()) {
            getLogger().info("Addon geladen: " + provider + " mit " + provider.getAllItems().size() + " Items.");
        }
    }

    private void registerPermissions() {
        for(String id : permissionItemRegistry.getWhitelistedItems().keySet()) {
            String permission = Permissions.VAULT.getLabel() + id;
            if(PermissionUtils.registerPermission(permission, "Vault item " + id, PermissionDefault.FALSE)) {
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
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);
        getServer().getPluginManager().registerEvents(new ItemAutoCollectListener(this), this);
        getServer().getPluginManager().registerEvents(new IslandListener(), this);
        getServer().getPluginManager().registerEvents(new VaultEventListener(adminVaultLogger), this);
    }

    public void loadAutoCollectPermissions() {
        radiusPermissionMap.clear();

        List<Integer> radii = getConfig().getIntegerList("auto-collect-radius");
        for (int radius : radii) {
            if (radius > 0) {
                String permission = Permissions.COLLECT_RADIUS.getLabel() + radius;
                if (PermissionUtils.registerPermission(permission, "Autocollect radius " + radius, PermissionDefault.FALSE)) {
                    //debugLogger.info("Registered autocolllect radius " + radius);
                } else {
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

    public void reloadPluginConfig(CommandSender sender) {
        reloadConfig();
        debug = getConfig().getBoolean("debug", false);
        if(!firstStart) {
            getLogger().info("Config geladen. Debugmodus ist " + (debug ? "aktiviert" : "deaktiviert"));
            firstStart = true;
        }

        if(sender != null) {
            sender.sendMessage("Config neu geladen. Debugmodus ist " + (debug ? "aktiviert" : "deaktiviert"));
        }
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
