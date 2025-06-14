package de.tebrox.islandVault;

import de.tebrox.islandVault.Commands.AdminCommand.VaultAdminMainCommand;
import de.tebrox.islandVault.Commands.VaultMainCommand;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Listeners.*;
import de.tebrox.islandVault.Manager.*;
import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Utils.*;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
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
    private static ItemManager itemManager;
    private static VaultManager vaultManager;
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

        MenuManager.setup(getServer(), this);

        languageManager = new LanguageManager(this);

        itemManager = new ItemManager(this);
        vaultManager = new VaultManager(this);

        reloadPluginConfig(null);

        registerCommandsAndEvents();

        loadAutoCollectPermissions();

        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : getServer().getOnlinePlayers()) {
                    IslandsManager islandsManager = IslandUtils.getIslandManager();
                    if(islandsManager == null) {
                        //debugLogger.warning("Cannot load onlineplayer islands. Islandmanager is null!");
                    }

                    Island island = islandsManager.getIsland(player.getWorld(), player.getUniqueId());
                    if(island != null) {
                        UUID ownerUUID = island.getOwner();

                        IslandVault.getVaultManager().loadVault(ownerUUID);
                    }
                }
            }
        }.runTaskLater(this, 40L);
    }

    @Override
    public void onDisable() {
        if(!getVaultManager().getVaults().isEmpty()) {
            for(Map.Entry<UUID, PlayerVaultUtils> entry : getVaultManager().getVaults().entrySet()) {
                IslandVault.getVaultManager().saveVault(entry.getValue());
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
    }

    private void registerPermissions() {
        ConfigurationSection section = config.getConfigurationSection(Permissions.GROUPS_CONFIG.getLabel());

        ItemGroupManager.init(this);

        for(Material material : itemManager.getMaterialList()) {
            String permission = Permissions.VAULT.getLabel() + material.toString().toLowerCase();
            if(PermissionUtils.registerPermission(permission, "Vault item " + material.toString(), PermissionDefault.FALSE)) {
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

    public void reloadPluginConfig(CommandSender sender) {
        reloadConfig();
        debug = getConfig().getBoolean("debug", false);
        getLogger().info("Config neu geladen. Debugmodus ist " + (debug ? "aktiviert" : "deaktiviert"));
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
