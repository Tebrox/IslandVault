package de.tebrox.islandVault;

import de.tebrox.islandVault.Commands.AdminCommand.VaultAdminMainCommand;
import de.tebrox.islandVault.Commands.VaultMainCommand;
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
    private static ItemManager itemManager;
    private static VaultManager vaultManager;
    FileConfiguration config = getConfig();
    private static MainCommand mainCommand;
    private static MainCommand adminMainCommand;
    //private Logger logger;
    private Map<String, Integer> radiusPermissionMap = new HashMap<>();
    private static LanguageManager languageManager;
    private boolean debug;

    public static Map<Location, VaultChest> customChests = new HashMap<>();
    public static Map<VaultChest, BukkitRunnable> runningVaultChestTasks = new HashMap<>();
    public static Map<String, List<VaultChest>> pendingChestsByWorldName;

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

        Bukkit.getPluginManager().registerEvents(new VaultChestListener(), this);
        VaultChestItem.registerRecipe(this);

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
                            System.out.println("Load user in luckperms cache: " + Bukkit.getOfflinePlayer(ownerUUID).getName());
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

        saveChests();
        for (BukkitRunnable task : runningVaultChestTasks.values()) {
            task.cancel();
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

        getServer().getPluginManager().registerEvents(new WorldLoadListener(this), this);
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

    public void loadChests() {
        FileConfiguration cfg = getConfig();
        if (!cfg.isConfigurationSection("chests")) return;

        for (String key : cfg.getConfigurationSection("chests").getKeys(false)) {
            String locString = cfg.getString("chests." + key + ".loc");
            if (locString == null || locString.isEmpty()) {
                getLogger().warning("Chest " + key + " hat keine Location, wird übersprungen.");
                continue;
            }

            String[] parts = locString.split(",");
            if (parts.length != 4) {
                getLogger().warning("Chest " + key + " Location fehlerhaft, wird übersprungen.");
                continue;
            }

            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            World world = Bukkit.getWorld(worldName);

            // Owner laden
            String ownerStr = cfg.getString("chests." + key + ".owner");
            if (ownerStr == null) {
                getLogger().warning("Chest " + key + " hat keinen Besitzer, wird übersprungen.");
                continue;
            }
            UUID owner = UUID.fromString(ownerStr);

            // Island laden
            String islandID = cfg.getString("chests." + key + ".islandID");
            Island island = null;
            if (islandID != null) {
                island = IslandUtils.getIslandManager().getIslandById(islandID).orElse(null);
                if (island == null) {
                    getLogger().warning("Island " + islandID + " der Chest " + key + " konnte nicht gefunden werden.");
                }
            }

            // VaultChest erstellen
            Location loc = (world != null) ? new Location(world, x, y, z) : new Location(null, x, y, z);
            VaultChest chest = new VaultChest(owner, loc, island);

            boolean inputMode = cfg.getString("chests." + key + ".mode").equals("input") ? true : false;
            chest.setMode(inputMode);

            // Filteritems laden
            ItemStack[] tempFilter = new ItemStack[9];
            if (cfg.isConfigurationSection("chests." + key + ".filter")) {
                for (int i = 0; i < 9; i++) {
                    ItemStack item = cfg.getItemStack("chests." + key + ".filter." + i);
                    if (item != null) tempFilter[i] = item;
                }
            }
            chest.setSavedFilter(tempFilter);

            // Welt vorhanden?
            if (world == null) {
                // Pending-Map
                pendingChestsByWorldName
                        .computeIfAbsent(worldName, k -> new ArrayList<>())
                        .add(chest);
                getLogger().info("Chest " + key + " wird geladen, sobald Welt " + worldName + " verfügbar ist.");
            } else {
                // Sofort laden
                customChests.put(loc, chest);
                getLogger().info("Chest " + key + " geladen: " + serializeLoc(loc));
            }
        }
    }



    public void saveChests() {
        FileConfiguration cfg = getConfig();

        // Kompletten Abschnitt "chests" zurücksetzen
        cfg.set("chests", null);

        // Wir gruppieren nach Weltname
        Map<String, Integer> worldIndexMap = new HashMap<>();

        for (VaultChest chest : customChests.values()) {
            Location loc = chest.getLocation();
            if (loc == null) {
                getLogger().warning("Chest ohne Location übersprungen.");
                continue;
            }

            World world = loc.getWorld();
            if (world == null) {
                getLogger().warning("Chest mit null-World übersprungen: " + loc);
                continue;
            }

            String worldName = world.getName();
            int index = worldIndexMap.getOrDefault(worldName, 0);
            String path = "chests." + worldName + "." + index;

            // Location
            cfg.set(path + ".loc", serializeLoc(loc));

            // Owner
            cfg.set(path + ".owner", chest.getOwner().toString());

            //InputMode
            cfg.set(path + ".mode", chest.isInputChest() ? "input" : "output");

            // Island
            if (chest.getIsland() != null) {
                cfg.set(path + ".islandID", chest.getIsland().getUniqueId().toString());
            }

            // Filter
            ItemStack[] filter = chest.getSavedFilter();
            if (filter != null) {
                for (int i = 0; i < filter.length; i++) {
                    if (filter[i] != null) {
                        cfg.set(path + ".filter." + i, filter[i]);
                    }
                }
            }

            // Index hochzählen
            worldIndexMap.put(worldName, index + 1);
        }

        saveConfig();
    }



    public String serializeLoc(Location loc) {
        if (loc == null) {
            return null;
        }

        String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : "UNKNOWN";
        return worldName + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public Location deserializeLoc(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        String[] parts = s.split(",");
        if (parts.length != 4) {
            return null;
        }

        String worldName = parts[0];
        World world = Bukkit.getWorld(worldName); // kann null sein, wenn Welt nicht geladen

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);

        return (world != null) ? new Location(world, x, y, z) : new Location(null, x, y, z);
    }
}
