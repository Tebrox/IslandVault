package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Items.VaultChest;
import de.tebrox.islandVault.Manager.VaultChestManager;
import de.tebrox.islandVault.Utils.IslandUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.database.objects.Island;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Chunk-basierter VaultChest-Loader.
 * Arbeitet mit dem VaultChestManager und dynamischen Dateien in /chestData/.
 */
public class ChunkLoadListener implements Listener {

    private final IslandVault plugin;
    private final VaultChestManager chestManager;

    // Index: worldName -> (chunkKey "x;z" -> List<chestKeys>)
    private final Map<String, Map<String, List<String>>> chunkIndex = new HashMap<>();

    public ChunkLoadListener(IslandVault plugin) {
        this.plugin = plugin;
        this.chestManager = IslandVault.getVaultChestManager();
        buildAllIndices();
    }

    /**
     * Erstellt für jede Welt einen Index ihrer Chests.
     */
    private void buildAllIndices() {
        File folder = new File(plugin.getDataFolder(), "chestData");
        if (!folder.exists()) folder.mkdirs();

        File[] worldFiles = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (worldFiles == null || worldFiles.length == 0) {
            plugin.getLogger().info("[ChunkLoader] Keine VaultChest-Daten gefunden.");
            return;
        }

        for (File file : worldFiles) {
            String worldName = file.getName().replace(".yml", "");
            ensureWorldDataExists(worldName);
            FileConfiguration cfg = plugin.getConfigManager().getConfig("chestData/" + worldName);
            buildWorldIndex(worldName, cfg);
        }

        plugin.getLogger().info("[ChunkLoader] VaultChest-Indizes für " + chunkIndex.size() + " Welten erstellt.");
    }

    /**
     * Erstellt eine chestData-Datei aus dem Template, falls sie fehlt.
     */
    private void ensureWorldDataExists(String worldName) {
        String path = "chestData/" + worldName;
        plugin.getConfigManager().createFromTemplateIfMissing(path, "chestData/template.yml");
    }

    private void buildWorldIndex(String worldName, FileConfiguration cfg) {
        ConfigurationSection chestsSection = cfg.getConfigurationSection("chests");
        if (chestsSection == null) return;

        Map<String, List<String>> chunkMap = new HashMap<>();

        for (String chestKey : chestsSection.getKeys(false)) {
            String locStr = cfg.getString("chests." + chestKey + ".loc");
            if (locStr == null) continue;

            Location loc = chestManager.deserializeLoc(locStr);
            if (loc == null) continue;

            int chunkX = loc.getBlockX() >> 4;
            int chunkZ = loc.getBlockZ() >> 4;
            String chunkKey = chunkX + ";" + chunkZ;

            chunkMap.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(chestKey);
        }

        chunkIndex.put(worldName, chunkMap);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        String worldName = world.getName();
        ensureWorldDataExists(worldName);

        int cx = chunk.getX();
        int cz = chunk.getZ();
        String chunkKey = cx + ";" + cz;

        Map<String, List<String>> worldChunks = chunkIndex.get(worldName);
        if (worldChunks == null) return;

        List<String> chestIds = worldChunks.get(chunkKey);
        if (chestIds == null) return;

        FileConfiguration cfg = IslandVault.getConfigManager().getConfig("chestData/" + worldName);

        for (String id : chestIds) {
            String basePath = "chests." + id;

            Location loc = chestManager.deserializeLoc(cfg.getString(basePath + ".loc"));
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
            ItemStack[] tempFilter = new ItemStack[9];
            ConfigurationSection filterSec = cfg.getConfigurationSection(basePath + ".filter");
            if (filterSec != null) {
                for (int i = 0; i < 9; i++) {
                    ItemStack item = cfg.getItemStack(basePath + ".filter." + i);
                    if (item != null) tempFilter[i] = item;
                }
            }
            chest.setSavedFilter(tempFilter);

            boolean inputMode = "input".equalsIgnoreCase(cfg.getString(basePath + ".mode"));
            chest.setMode(inputMode);

            chestManager.registerChest(chest);
            //plugin.getLogger().log(Level.FINE, "[ChunkLoader] VaultChest geladen: " + loc + " (" + worldName + ")");
        }

        //plugin.getLogger().log(Level.INFO, "[ChunkLoader] Chunk [" + cx + "," + cz + "] in Welt " + worldName + " → " + chestIds.size() + " VaultChests aktiviert.");
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        String worldName = chunk.getWorld().getName();
        ensureWorldDataExists(worldName);

        int cx = chunk.getX();
        int cz = chunk.getZ();

        List<VaultChest> toRemove = new ArrayList<>();

        for (VaultChest chest : chestManager.getActiveChests()) {
            Location loc = chest.getLocation();
            if (!loc.getWorld().getName().equals(worldName)) continue;

            if ((loc.getBlockX() >> 4) == cx && (loc.getBlockZ() >> 4) == cz) {
                chestManager.saveChest(chest);
                toRemove.add(chest);
            }
        }

        for (VaultChest chest : toRemove) {
            chestManager.removeChest(chest.getLocation());
        }

        if (!toRemove.isEmpty()) {
            //plugin.getLogger().info("[ChunkLoader] Chunk [" + cx + "," + cz + "] entladen → " + toRemove.size() + " VaultChests gespeichert und entfernt.");
        }

        IslandVault.getConfigManager().saveConfig("chestData/" + worldName);
    }

    public void rebuildIndex() {
        chunkIndex.clear();
        buildAllIndices();
        //plugin.getLogger().info("[ChunkLoader] VaultChest-Indizes neu erstellt (Reload).");
    }
}
