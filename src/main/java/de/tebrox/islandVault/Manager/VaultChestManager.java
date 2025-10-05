package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Items.VaultChest;
import de.tebrox.islandVault.Listeners.VaultChestListener;
import de.tebrox.islandVault.Utils.IslandUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import world.bentobox.bentobox.database.objects.Island;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Verantwortlich für das Laden, Speichern und Verwalten aller VaultChests.
 * Arbeitet mit ConfigManager und dynamischen Dateien in /chestData/.
 */
public class VaultChestManager {

    private final IslandVault plugin;
    private final Map<Location, VaultChest> activeChests = new HashMap<>();
    private final Map<VaultChest, BukkitRunnable> runningTasks = new HashMap<>();
    private final Queue<VaultChest> saveQueue = new ConcurrentLinkedQueue<>();
    private BukkitRunnable asyncSaveTask;

    // 🔹 Neue Map: Kisten aus noch nicht geladenen Welten
    private final Map<String, List<VaultChest>> pendingChests = new HashMap<>();

    private ConfigManager configManager() {
        return IslandVault.getConfigManager();
    }

    public VaultChestManager(IslandVault plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(new VaultChestListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new WorldLoadListener(), plugin);
    }

    // ======================================================
    // =============== Laden & Speichern ====================
    // ======================================================

    public void loadChestsForWorld(String worldName) {
        configManager().createFromTemplateIfMissing("chestData/" + worldName, "chestData/template.yml");
        FileConfiguration cfg = configManager().getConfig("chestData/" + worldName);
        ConfigurationSection chests = cfg.getConfigurationSection("chests");

        if (chests == null || chests.getKeys(false).isEmpty()) return;

        World world = Bukkit.getWorld(worldName);
        int count = 0;

        for (String id : chests.getKeys(false)) {
            String basePath = "chests." + id;
            Location loc = deserializeLoc(cfg.getString(basePath + ".loc"));
            if (loc == null) continue;

            String ownerStr = cfg.getString(basePath + ".owner");
            if (ownerStr == null) continue;
            UUID owner = UUID.fromString(ownerStr);

            String islandId = cfg.getString(basePath + ".islandID");
            Optional<Island> islandOpt = (islandId != null)
                    ? IslandUtils.getIslandManager().getIslandById(islandId)
                    : Optional.empty();

            VaultChest chest = new VaultChest(owner, loc, islandOpt.orElse(null));

            // Filter laden
            ConfigurationSection filterSec = cfg.getConfigurationSection(basePath + ".filter");
            if (filterSec != null) {
                ItemStack[] tempFilter = new ItemStack[9];
                for (int i = 0; i < 9; i++) {
                    ItemStack item = cfg.getItemStack(basePath + ".filter." + i);
                    if (item != null) tempFilter[i] = item;
                }
                chest.setSavedFilter(tempFilter);
            }

            boolean inputMode = "input".equalsIgnoreCase(cfg.getString(basePath + ".mode"));
            chest.setMode(inputMode);

            // ✅ Wenn Welt geladen → direkt registrieren
            if (world != null) {
                registerChest(chest);
                count++;
            } else {
                // 💤 Wenn Welt noch nicht geladen → in Pending-Map eintragen
                pendingChests.computeIfAbsent(worldName, k -> new ArrayList<>()).add(chest);
            }
        }

        if (world != null) {
            plugin.getLogger().info("[VaultChestManager] " + count + " VaultChests in Welt '" + worldName + "' geladen.");
        } else {
            plugin.getLogger().warning("[VaultChestManager] Welt '" + worldName + "' noch nicht geladen — VaultChests werden später initialisiert.");
        }
    }

    // ======================================================
    // =============== Welt-Nachlade-Listener ===============
    // ======================================================

    private class WorldLoadListener implements Listener {
        @EventHandler
        public void onWorldLoad(WorldLoadEvent event) {
            String worldName = event.getWorld().getName();
            List<VaultChest> list = pendingChests.remove(worldName);
            if (list == null || list.isEmpty()) return;

            int count = 0;
            for (VaultChest chest : list) {
                Location loc = chest.getLocation();
                if (loc.getWorld() == null)
                    loc.setWorld(event.getWorld());
                registerChest(chest);
                count++;
            }

            plugin.getLogger().info("[VaultChestManager] " + count + " VaultChests nachgeladen für Welt '" + worldName + "'.");
        }
    }

    // ======================================================
    // =============== Laden & Speichern ====================
    // ======================================================

    /**
     * Lädt alle Truhen aus /chestData/<welt>.yml
     */
    public void loadAllChests() {
        plugin.getLogger().info("[VaultChestManager] Lade alle VaultChests ...");

        File folder = new File(plugin.getDataFolder(), "chestData");
        if (!folder.exists() || !folder.isDirectory()) {
            plugin.getLogger().info("[VaultChestManager] Kein Ordner 'chestData' gefunden – nichts zu laden.");
            return;
        }

        List<String> processed = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("[VaultChestManager] Keine chestData-Dateien gefunden.");
            return;
        }

        for (File f : files) {
            String fileName = f.getName();
            String worldName = fileName.substring(0, fileName.length() - 4); // ".yml" abtrennen
            loadChestsForWorld(worldName);
            processed.add(worldName);
        }

        plugin.getLogger().info("[VaultChestManager] VaultChests geladen für: " + String.join(", ", processed));
    }


    /**
     * Speichert alle Truhen in ihren jeweiligen Welt-Dateien.
     */
    public void saveAllChests() {
        plugin.getLogger().info("[VaultChestManager] Speichere alle VaultChests ...");
        Map<String, List<VaultChest>> byWorld = new HashMap<>();

        for (VaultChest chest : activeChests.values()) {
            byWorld.computeIfAbsent(chest.getLocation().getWorld().getName(), k -> new ArrayList<>()).add(chest);
        }

        for (Map.Entry<String, List<VaultChest>> entry : byWorld.entrySet()) {
            saveChestsForWorld(entry.getKey(), entry.getValue());
        }
        plugin.getLogger().info("[VaultChestManager] Alle VaultChests gespeichert.");
    }

    /**
     * Fügt eine VaultChest zur asynchronen Speicherwarteschlange hinzu.
     * Der tatsächliche Schreibvorgang erfolgt im Hintergrund.
     */
    public void saveChest(VaultChest chest) {
        saveQueue.add(chest);

        // Starte den Hintergrund-Task, falls er noch nicht läuft
        if (asyncSaveTask == null) {
            startAsyncSaveTask();
        }
    }

    /**
     * Führt den eigentlichen Speichervorgang synchron aus (Haupt-Thread oder intern).
     * Sollte NICHT direkt aufgerufen werden, außer bei Plugin-Stop.
     */
    private void saveChestImmediate(VaultChest chest) {
        try {
            String worldName = chest.getLocation().getWorld().getName();
            String path = "chestData/" + worldName;

            configManager().createFromTemplateIfMissing(path, "chestData/template.yml");
            FileConfiguration cfg = configManager().getConfig(path);

            // Bestehenden Eintrag anhand Location suchen
            ConfigurationSection chestsSec = cfg.getConfigurationSection("chests");
            if (chestsSec == null) {
                chestsSec = cfg.createSection("chests");
            }

            String existingKey = null;
            for (String key : chestsSec.getKeys(false)) {
                String locStr = cfg.getString("chests." + key + ".loc");
                if (locStr != null && locStr.equalsIgnoreCase(serializeLoc(chest.getLocation()))) {
                    existingKey = key;
                    break;
                }
            }

            // Wenn keine ID existiert → neue generieren
            if (existingKey == null) {
                existingKey = UUID.randomUUID().toString().substring(0, 8);
            }

            String base = "chests." + existingKey;
            cfg.set(base + ".loc", serializeLoc(keyOf(chest.getLocation())));
            cfg.set(base + ".owner", chest.getOwner().toString());
            cfg.set(base + ".mode", chest.isInputChest() ? "input" : "output");

            if (chest.getIsland() != null)
                cfg.set(base + ".islandID", chest.getIsland().getUniqueId());

            ItemStack[] filter = chest.getSavedFilter();
            if (filter != null) {
                for (int i = 0; i < filter.length; i++) {
                    if (filter[i] != null) {
                        cfg.set(base + ".filter." + i, filter[i]);
                    }
                }
            }

            configManager().saveConfig(path);
            plugin.getLogger().fine("[VaultChestManager] VaultChest gespeichert: " + chest.getLocation());

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[VaultChestManager] Fehler beim Speichern einer VaultChest!", e);
        }
    }

    /**
     * Entfernt eine VaultChest vollständig aus der zugehörigen Config-Datei.
     * Wird beim Zerstören einer Truhe aufgerufen.
     */
    public void deleteChest(VaultChest chest) {
        try {
            String worldName = chest.getLocation().getWorld().getName();
            String path = "chestData/" + worldName;

            configManager().createFromTemplateIfMissing(path, "chestData/template.yml");
            FileConfiguration cfg = configManager().getConfig(path);

            ConfigurationSection chestsSec = cfg.getConfigurationSection("chests");
            if (chestsSec == null) return;

            String targetLoc = serializeLoc(chest.getLocation());
            boolean removed = false;

            for (String key : new ArrayList<>(chestsSec.getKeys(false))) {
                String locStr = cfg.getString("chests." + key + ".loc");
                if (locStr != null && locStr.equalsIgnoreCase(targetLoc)) {
                    cfg.set("chests." + key, null);
                    removed = true;
                    break;
                }
            }

            if (removed) {
                configManager().saveConfig(path);
                plugin.getLogger().fine("[VaultChestManager] VaultChest entfernt aus " + path + " @ " + targetLoc);
            } else {
                plugin.getLogger().fine("[VaultChestManager] Keine passende Chest gefunden zum Löschen @ " + targetLoc);
            }

            // Entferne aus RAM & Save-Queue
            activeChests.remove(chest.getLocation());
            saveQueue.remove(chest);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[VaultChestManager] Fehler beim Löschen einer VaultChest!", e);
        }
    }


    /**
     * Startet einen asynchronen Task, der die Warteschlange regelmäßig leert.
     */
    private void startAsyncSaveTask() {
        asyncSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;
                while (!saveQueue.isEmpty() && processed < 10) { // max 10 pro Tick
                    VaultChest chest = saveQueue.poll();
                    if (chest != null) {
                        // Wir führen den Schreibvorgang synchron aus,
                        // aber die Queue läuft asynchron außerhalb des Main Threads.
                        Bukkit.getScheduler().runTask(plugin, () -> saveChestImmediate(chest));
                        processed++;
                    }
                }

                if (saveQueue.isEmpty()) {
                    cancel();
                    asyncSaveTask = null;
                }
            }
        };
        asyncSaveTask.runTaskTimerAsynchronously(plugin, 20L, 20L); // alle 1s prüfen
    }

    /**
     * Stoppt den Hintergrundtask und speichert verbleibende Chests sofort.
     */
    public void flushAndStopAsyncSave() {
        if (asyncSaveTask != null) {
            asyncSaveTask.cancel();
            asyncSaveTask = null;
        }

        VaultChest chest;
        while ((chest = saveQueue.poll()) != null) {
            saveChestImmediate(chest);
        }

        plugin.getLogger().info("[VaultChestManager] Alle wartenden VaultChests wurden vor dem Shutdown gespeichert.");
    }

    /**
     * Speichert alle Truhen für eine bestimmte Welt.
     */
    public void saveChestsForWorld(String worldName, List<VaultChest> list) {
        configManager().createFromTemplateIfMissing("chestData/" + worldName, "chestData/template.yml");
        FileConfiguration cfg = configManager().getConfig("chestData/" + worldName);
        cfg.set("chests", null); // clear old data

        for (int i = 0; i < list.size(); i++) {
            VaultChest chest = list.get(i);
            String base = "chests." + i;

            cfg.set(base + ".loc", serializeLoc(chest.getLocation()));
            cfg.set(base + ".owner", chest.getOwner().toString());

            if (chest.getIsland() != null)
                cfg.set(base + ".islandID", chest.getIsland().getUniqueId());

            cfg.set(base + ".mode", chest.isInputChest() ? "input" : "output");

            ItemStack[] filter = chest.getSavedFilter();
            if (filter != null) {
                for (int slot = 0; slot < filter.length; slot++) {
                    if (filter[slot] != null)
                        cfg.set(base + ".filter." + slot, filter[slot]);
                }
            }
        }

        configManager().saveConfig("chestData/" + worldName);
        plugin.getLogger().info("[VaultChestManager] " + list.size() + " VaultChests in Welt '" + worldName + "' gespeichert.");
    }

    // ======================================================
    // =============== Verwaltung & Zugriff =================
    // ======================================================

    public void registerChest(VaultChest chest) {
        Location key = keyOf(chest.getLocation());
        if (key == null) return;
        activeChests.put(chest.getLocation(), chest);
        if(!runningTasks.containsKey(chest)) {
            startAutoTask(chest, VaultChestListener.createChestTask(chest));
        }

    }

    public void removeChest(Location loc) {
        Location key = keyOf(loc);
        if (key == null) return;
        if(runningTasks.containsKey(getChestAt(loc))) {
            stopAutoTask(getChestAt(loc));
        }


        activeChests.remove(loc);
    }

    public VaultChest getChestAt(Location loc) {
        Location key = keyOf(loc);
        if (key == null) return null;

        return activeChests.get(loc);
    }

    public Collection<VaultChest> getActiveChests() {
        return activeChests.values();
    }

    // ======================================================
    // =============== Hintergrund-Tasks ====================
    // ======================================================

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

    public VaultChest getVaultChestFromHolder(InventoryHolder holder) {
        if (holder == null) return null;

        Location loc = null;
        if (holder instanceof Chest chestBlock) {
            loc = chestBlock.getLocation();
        } else if (holder instanceof DoubleChest doubleChest) {
            loc = doubleChest.getLocation();
        }

        return (loc != null)
                ? IslandVault.getVaultChestManager().getChestAt(loc)
                : null;
    }

    private static Location keyOf(Location loc) {
        if (loc == null) return null;
        World w = loc.getWorld();
        if (w == null) return null;
        return new Location(w, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

}
