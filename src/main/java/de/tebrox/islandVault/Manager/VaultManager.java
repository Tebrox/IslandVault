package de.tebrox.islandVault.Manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Utils.ItemStackKey;
import de.tebrox.islandVault.Utils.ItemStackKeyMapAdapter;
import de.tebrox.islandVault.Utils.VaultDataConverter;
import de.tebrox.islandVault.VaultData;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages vault data for islands, including asynchronous loading, saving,
 * caching, and migration from old YAML format to JSON.
 *
 * Vaults are stored per island and owner UUID, and cached in memory for fast access.
 */
public class VaultManager {

    private final IslandVault plugin;
    private final File vaultFolder;
    private final Gson gson;

    private final Map<String, VaultData> cache = new ConcurrentHashMap<>();

    /**
     * Constructs a VaultManager for the given plugin instance,
     * creating the vault storage folder if necessary.
     *
     * @param plugin the plugin instance
     */
    public VaultManager(IslandVault plugin) {
        this.plugin = plugin;
        this.vaultFolder = new File(plugin.getDataFolder(), "vaults");
        if (!vaultFolder.exists() && !vaultFolder.mkdirs()) {
            throw new IllegalStateException("Kann Verzeichnis " + vaultFolder + " nicht anlegen.");
        }

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Asynchronously gets the VaultData for the given island ID and owner UUID.
     * Returns cached data if available, otherwise loads or creates a new vault.
     *
     * @param islandId the island ID
     * @param ownerUUID the owner's UUID
     * @return a CompletableFuture completing with the VaultData or null on failure
     */
    public CompletableFuture<VaultData> getVaultAsync(String islandId, UUID ownerUUID) {
        // Cache-Hit
        if (cache.containsKey(islandId)) {
            return CompletableFuture.completedFuture(cache.get(islandId));
        }

        // Async-Laden oder Neu-Erstellen
        return CompletableFuture.supplyAsync(() -> loadOrCreateVault(islandId, ownerUUID));
    }

    /**
     * Asynchronously saves the vault data associated with the given island ID and owner name.
     *
     * @param islandId the island ID
     * @param ownerName the owner's name (currently unused in save method)
     */
    public void saveVaultAsync(String islandId, String ownerName) {
        VaultData data = cache.get(islandId);
        saveVaultAsync(data);
    }

    /**
     * Asynchronously saves the given VaultData to disk.
     *
     * @param data the VaultData to save
     */
    public void saveVaultAsync(VaultData data) {
        if (data == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                saveVaultToFile(data.getIslandId(), data.getOwnerUUID(), data);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Unloads the vault from cache and saves it synchronously.
     *
     * @param islandId the island ID
     * @param ownerUUID the owner's UUID
     */
    public void unloadVault(String islandId, UUID ownerUUID) {
        VaultData data = cache.remove(islandId);
        if (data != null) {
            saveVaultToFile(islandId, ownerUUID, data);
        }
    }

    /**
     * Saves all cached vaults to disk and clears the cache.
     */
    public void flushAll() {
        for (Map.Entry<String, VaultData> entry : cache.entrySet()) {
            String islandId = entry.getKey();
            VaultData data = entry.getValue();
            saveVaultToFile(islandId, data.getOwnerUUID(), data);
        }
        cache.clear();
    }

    /**
     * Loads a VaultData from disk or creates a new one if none exists.
     * Converts old YAML format to JSON if necessary.
     *
     * @param islandId the island ID
     * @param ownerUUID the owner's UUID
     * @return the loaded or newly created VaultData, or null if loading failed
     */
    public VaultData loadOrCreateVault(String islandId, UUID ownerUUID) {
        File jsonFile = getJsonFile(ownerUUID, islandId);
        File yamlFile = getYamlFile(ownerUUID, islandId);

        // Konvertieren, falls YAML vorhanden
        if (yamlFile.exists() && !jsonFile.exists()) {
            boolean converted = new VaultDataConverter(plugin).convertIfOldFormat(ownerUUID, islandId, yamlFile);
            if (!converted) {
                plugin.getLogger().warning("Migration fehlgeschlagen für Vault von " + Bukkit.getOfflinePlayer(ownerUUID).getName() + " (" + islandId + ")");
                return null; // <-- WICHTIG: Fehler zurückgeben
            }
        }

        // Jetzt JSON laden oder neu anlegen
        VaultData data;
        if (jsonFile.exists()) {
            try (Reader reader = new FileReader(jsonFile)) {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(new TypeToken<Map<ItemStackKey, Integer>>(){}.getType(), new ItemStackKeyMapAdapter())
                        .create();
                data = gson.fromJson(reader, VaultData.class);
                if (data == null) data = new VaultData(islandId, ownerUUID);
            } catch (IOException ex) {
                plugin.getLogger().warning("Fehler beim Laden von " + jsonFile.getName() + ": " + ex.getMessage());
                return null;
            }
        } else {
            data = new VaultData(islandId, ownerUUID);
        }

        cache.put(islandId, data);
        return data;
    }

    private void saveVaultToFile(String islandId, UUID ownerUUID, VaultData data) {
        File jsonFile = getJsonFile(ownerUUID, islandId);
        try (Writer writer = new FileWriter(jsonFile)) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(new TypeToken<Map<ItemStackKey, Integer>>(){}.getType(), new ItemStackKeyMapAdapter())
                    .setPrettyPrinting()
                    .create();
            gson.toJson(data, writer);
        } catch (IOException ex) {
            plugin.getLogger().severe("Konnte Vault " + jsonFile.getName() + " nicht speichern: " + ex.getMessage());
        }
    }

    private File getJsonFile(UUID ownerUUID, String islandId) {
        String owner = Bukkit.getOfflinePlayer(ownerUUID).getName();
        return new File(vaultFolder, owner + "_" + islandId + ".json");
    }

    private File getYamlFile(UUID ownerUUID, String islandId) {
        String owner = Bukkit.getOfflinePlayer(ownerUUID).getName();
        return new File(vaultFolder, Bukkit.getOfflinePlayer(owner).getUniqueId() + ".yml");

    }

    private String resolveOwnerName(String islandId) {
        try {
            Island island = BentoBox.getInstance().getIslands().getIslandById(islandId).orElse(null);
            if (island != null && island.getOwner() != null) {
                return Bukkit.getOfflinePlayer(island.getOwner()).getName();
            }
        } catch (Exception e) {
            // Fehler ignorieren
        }
        return "unknownOwner";
    }

    /**
     * Checks if a vault for the given owner name is currently cached.
     *
     * @param ownerName the owner's name
     * @return true if a vault for the owner is cached, false otherwise
     */
    public boolean isOwnerInCache(String ownerName) {
        return cache.values().stream()
                .anyMatch(data -> ownerName.equalsIgnoreCase(data.getOwnerName()));
    }

    /**
     * Checks if a vault for the given island ID is currently loaded in cache.
     *
     * @param islandId the island ID
     * @return true if loaded, false otherwise
     */
    public boolean isVaultLoaded(String islandId) {
        return cache.containsKey(islandId);
    }

    /**
     * Gets the cached vault for the given island ID.
     *
     * @param islandId the island ID
     * @return the cached VaultData or null if not loaded
     */
    public VaultData getCachedVault(String islandId) {
        return cache.get(islandId);
    }

    public Map<String, VaultData> getCache() {
        return cache;
    }
}