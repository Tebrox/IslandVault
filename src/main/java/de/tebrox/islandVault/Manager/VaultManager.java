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

public class VaultManager {

    private final IslandVault plugin;
    private final File vaultFolder;
    private final Gson gson;

    private final Map<String, VaultData> cache = new ConcurrentHashMap<>();

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

    public CompletableFuture<VaultData> getVaultAsync(String islandId, UUID ownerUUID) {
        // Cache-Hit
        if (cache.containsKey(islandId)) {
            return CompletableFuture.completedFuture(cache.get(islandId));
        }

        // Async-Laden oder Neu-Erstellen
        return CompletableFuture.supplyAsync(() -> loadOrCreateVault(islandId, ownerUUID));
    }

    public void saveVaultAsync(String islandId, String ownerName) {
        VaultData data = cache.get(islandId);
        saveVaultAsync(data);
    }

    public void saveVaultAsync(VaultData data) {
        if (data == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                saveVaultToFile(data.getIslandId(), data.getOwnerUUID(), data);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void unloadVault(String islandId, UUID ownerUUID) {
        VaultData data = cache.remove(islandId);
        if (data != null) {
            saveVaultToFile(islandId, ownerUUID, data);
        }
    }

    public void flushAll() {
        for (Map.Entry<String, VaultData> entry : cache.entrySet()) {
            String islandId = entry.getKey();
            VaultData data = entry.getValue();
            saveVaultToFile(islandId, data.getOwnerUUID(), data);
        }
        cache.clear();
    }

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

    public boolean isOwnerInCache(String ownerName) {
        return cache.values().stream()
                .anyMatch(data -> ownerName.equalsIgnoreCase(data.getOwnerName()));
    }

    public boolean isVaultLoaded(String islandId) {
        return cache.containsKey(islandId);
    }

    public VaultData getCachedVault(String islandId) {
        return cache.get(islandId);
    }

    public Map<String, VaultData> getCache() {
        return cache;
    }
}