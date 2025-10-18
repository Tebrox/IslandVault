package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Items.VaultChest;
import de.tebrox.islandVault.Listeners.VaultChestListener;
import de.tebrox.islandVault.Utils.IslandUtils;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.database.objects.Island;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Verwalter für alle VaultChests – zuständig für
 * Laden, Speichern und Hintergrundtasks.
 * Inselbasiertes System (islandId statt ownerUUID).
 */
public class VaultChestManager {

    private final IslandVault plugin;
    private final Map<Location, VaultChest> activeChests = new HashMap<>();
    private final Map<VaultChest, BukkitRunnable> runningTasks = new HashMap<>();
    private final Queue<VaultChest> saveQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, List<VaultChest>> pendingChests = new HashMap<>();

    private BukkitRunnable asyncSaveTask;

    private ConfigManager configManager() {
        return IslandVault.getConfigManager();
    }

    public VaultChestManager(IslandVault plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new VaultChestListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new WorldLoadListener(), plugin);
    }

    // ============================================================
    // ======================= Laden ==============================
    // ============================================================

    /**
     * Lädt alle VaultChests aus /chestData/<world>.yml
     */
    public void loadChestsForWorld(String worldName) {
        configManager().createFromTemplateIfMissing("chestData/" + worldName, "chestData/template.yml");
        FileConfiguration cfg = configManager().getConfig("chestData/" + worldName);
        ConfigurationSection chests = cfg.getConfigurationSection("chests");

        if (chests == null || chests.getKeys(false).isEmpty()) return;

        World world = Bukkit.getWorld(worldName);
        int count = 0;

        for (String id : chests.getKeys(false)) {
            String base = "chests." + id;
            Location loc = deserializeLoc(cfg.getString(base + ".loc"));
            if (loc == null) continue;

            String islandId = cfg.getString(base + ".islandID");
            Optional<Island> islandOpt = (islandId != null)
                    ? IslandUtils.getIslandManager().getIslandById(islandId)
                    : Optional.empty();

            VaultChest chest = new VaultChest(null, loc, islandOpt.orElse(null));

            // Filter laden
            ConfigurationSection filterSec = cfg.getConfigurationSection(base + ".filter");
            if (filterSec != null) {
                ItemStack[] tempFilter = new ItemStack[9];
                for (int i = 0; i < 9; i++) {
                    ItemStack item = cfg.getItemStack(base + ".filter." + i);
                    if (item != null) tempFilter[i] = item;
                }
                chest.setSavedFilter(tempFilter);
            }

            boolean input = "input".equalsIgnoreCase(cfg.getString(base + ".mode"));
            chest.setMode(input);

            if (world != null) {
                registerChest(chest);
                count++;
            } else {
                pendingChests.computeIfAbsent(worldName, k -> new ArrayList<>()).add(chest);
            }
        }

        if (world != null)
            plugin.getLogger().info("[VaultChestManager] " + count + " VaultChests in '" + worldName + "' geladen.");
        else
            plugin.getLogger().warning("[VaultChestManager] Welt '" + worldName + "' noch nicht geladen – VaultChests werden später initialisiert.");
    }

    private class WorldLoadListener implements Listener {
        @EventHandler
        public void onWorldLoad(WorldLoadEvent event) {
            String worldName = event.getWorld().getName();
            List<VaultChest> list = pendingChests.remove(worldName);
            if (list == null || list.isEmpty()) return;

            for (VaultChest chest : list) {
                Location loc = chest.getLocation();
                if (loc.getWorld() == null) loc.setWorld(event.getWorld());
                registerChest(chest);
            }
            plugin.getLogger().info("[VaultChestManager] " + list.size() + " VaultChests nachgeladen für '" + worldName + "'.");
        }
    }

    public void loadAllChests() {
        File folder = new File(plugin.getDataFolder(), "chestData");
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String world = file.getName().replace(".yml", "");
            loadChestsForWorld(world);
        }
    }

    // ============================================================
    // ======================= Speichern ==========================
    // ============================================================

    public void saveChest(VaultChest chest) {
        saveQueue.add(chest);
        if (asyncSaveTask == null) startAsyncSaveTask();
    }

    private void saveChestImmediate(VaultChest chest) {
        try {
            String worldName = chest.getLocation().getWorld().getName();
            String path = "chestData/" + worldName;
            configManager().createFromTemplateIfMissing(path, "chestData/template.yml");
            FileConfiguration cfg = configManager().getConfig(path);

            ConfigurationSection sec = cfg.getConfigurationSection("chests");
            if (sec == null) sec = cfg.createSection("chests");

            String chestId = findChestKey(cfg, chest.getLocation());
            if (chestId == null) chestId = UUID.randomUUID().toString().substring(0, 8);

            String base = "chests." + chestId;
            cfg.set(base + ".loc", serializeLoc(chest.getLocation()));
            cfg.set(base + ".islandID", chest.getIsland() != null ? chest.getIsland().getUniqueId() : null);
            cfg.set(base + ".mode", chest.isInputChest() ? "input" : "output");

            ItemStack[] filter = chest.getSavedFilter();
            if (filter != null) {
                for (int i = 0; i < filter.length; i++) {
                    if (filter[i] != null)
                        cfg.set(base + ".filter." + i, filter[i]);
                }
            }

            configManager().markDirty(path);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[VaultChestManager] Fehler beim Speichern der VaultChest!", e);
        }
    }

    private String findChestKey(FileConfiguration cfg, Location loc) {
        ConfigurationSection sec = cfg.getConfigurationSection("chests");
        if (sec == null) return null;
        String target = serializeLoc(loc);
        for (String key : sec.getKeys(false)) {
            if (target.equalsIgnoreCase(cfg.getString("chests." + key + ".loc"))) return key;
        }
        return null;
    }

    private void startAsyncSaveTask() {
        asyncSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;
                while (!saveQueue.isEmpty() && processed < 10) {
                    VaultChest chest = saveQueue.poll();
                    if (chest != null)
                        Bukkit.getScheduler().runTask(plugin, () -> saveChestImmediate(chest));
                    processed++;
                }

                if (saveQueue.isEmpty()) {
                    cancel();
                    asyncSaveTask = null;
                }
            }
        };
        asyncSaveTask.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    public void flushAndStopAsyncSave() {
        if (asyncSaveTask != null) {
            asyncSaveTask.cancel();
            asyncSaveTask = null;
        }
        VaultChest chest;
        while ((chest = saveQueue.poll()) != null) saveChestImmediate(chest);
        plugin.getLogger().info("[VaultChestManager] Alle wartenden VaultChests gespeichert.");
    }

    // ============================================================
    // ======================= Verwaltung =========================
    // ============================================================

    public void registerChest(VaultChest chest) {
        Location key = keyOf(chest.getLocation());
        if (key == null) return;

        activeChests.put(key, chest);
        if (!runningTasks.containsKey(chest))
            startAutoTask(chest, VaultChestListener.createChestTask(chest));
    }

    public void removeChest(Location loc) {
        VaultChest chest = getChestAt(loc);
        if (chest != null) stopAutoTask(chest);
        activeChests.remove(keyOf(loc));
    }

    public VaultChest getChestAt(Location loc) {
        return activeChests.get(keyOf(loc));
    }

    public Collection<VaultChest> getActiveChests() {
        return activeChests.values();
    }

    // ============================================================
    // ======================= Auto-Tasks =========================
    // ============================================================

    public void startAutoTask(VaultChest chest, BukkitRunnable task) {
        if (runningTasks.containsKey(chest)) return;
        runningTasks.put(chest, task);
        task.runTaskTimer(plugin, 20L, 8L);
    }

    public void stopAutoTask(VaultChest chest) {
        BukkitRunnable task = runningTasks.remove(chest);
        if (task != null) task.cancel();
    }

    public void stopAllTasks() {
        runningTasks.values().forEach(BukkitRunnable::cancel);
        runningTasks.clear();
    }

    // ============================================================
    // ======================= Utilities ==========================
    // ============================================================

    private static Location keyOf(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public String serializeLoc(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public Location deserializeLoc(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(",");
        if (parts.length != 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);

        return (world != null) ? new Location(world, x, y, z) : new Location(null, x, y, z);
    }

    public VaultChest getVaultChestFromHolder(InventoryHolder holder) {
        if (holder == null) return null;
        Location loc = (holder instanceof Chest c) ? c.getLocation()
                : (holder instanceof DoubleChest dc ? dc.getLocation() : null);
        return (loc != null) ? getChestAt(loc) : null;
    }

    /**
     * Entfernt eine VaultChest vollständig:
     * - stoppt den Auto-Task
     * - löscht den Eintrag aus chestData/<world>.yml
     * - entfernt sie aus RAM (activeChests, saveQueue, pendingChests)
     */
    public void deleteChest(VaultChest chest) {
        if (chest == null || chest.getLocation() == null || chest.getLocation().getWorld() == null) return;

        try {
            // 1) Task stoppen & aus RAM-Maps entfernen
            stopAutoTask(chest);
            Location key = keyOf(chest.getLocation());
            if (key != null) activeChests.remove(key);
            saveQueue.remove(chest);

            // auch aus Pending entfernen (falls Welt noch nicht geladen war)
            String worldName = chest.getLocation().getWorld().getName();
            List<VaultChest> pending = pendingChests.get(worldName);
            if (pending != null) {
                pending.removeIf(c -> serializeLoc(c.getLocation()).equalsIgnoreCase(serializeLoc(chest.getLocation())));
                if (pending.isEmpty()) pendingChests.remove(worldName);
            }

            // 2) Eintrag in Datei entfernen
            String path = "chestData/" + worldName;
            configManager().createFromTemplateIfMissing(path, "chestData/template.yml");
            FileConfiguration cfg = configManager().getConfig(path);

            ConfigurationSection sec = cfg.getConfigurationSection("chests");
            if (sec != null) {
                String targetLoc = serializeLoc(chest.getLocation());
                boolean removed = false;

                for (String id : new ArrayList<>(sec.getKeys(false))) {
                    String locStr = cfg.getString("chests." + id + ".loc");
                    if (targetLoc.equalsIgnoreCase(locStr)) {
                        cfg.set("chests." + id, null);
                        removed = true;
                        break;
                    }
                }

                if (removed) {
                    configManager().markDirty(path);
                    plugin.getLogger().fine("[VaultChestManager] VaultChest gelöscht @ " + targetLoc + " (" + worldName + ")");
                } else {
                    plugin.getLogger().fine("[VaultChestManager] Kein Eintrag gefunden zum Löschen @ " + targetLoc + " (" + worldName + ")");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[VaultChestManager] Fehler beim Löschen einer VaultChest!", e);
        }
    }

    /** Convenience: löscht eine Truhe an der Position, falls vorhanden. */
    public void deleteChestAt(Location loc) {
        VaultChest chest = getChestAt(loc);
        if (chest != null) deleteChest(chest);
    }

}
