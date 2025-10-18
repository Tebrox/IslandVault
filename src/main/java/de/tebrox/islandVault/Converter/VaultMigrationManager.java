package de.tebrox.islandVault.Converter;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Führt einmalige Migration von Spieler-Vaults (playerUUID.yml)
 * zu Insel-Vaults (island_<uuid>.yml) durch.
 *
 * Voraussetzungen:
 *  - BentoBox API aktiv
 *  - Insel-UUIDs eindeutig für Spieler
 */
public class VaultMigrationManager {

    private final IslandVault plugin;
    private final BentoBox bentoBox;

    public VaultMigrationManager(IslandVault plugin) {
        this.plugin = plugin;
        this.bentoBox = BentoBox.getInstance();
    }

    /**
     * Führt die Migration aller alten Vault-Dateien aus.
     * Kann beim Start oder manuell aufgerufen werden.
     */
    public void migratePlayerVaultsToIslandVaults() {
        File folder = new File(plugin.getDataFolder(), "vaults");
        if (!folder.exists()) {
            plugin.getLogger().info("[Migration] Kein vaults/-Ordner vorhanden, überspringe Migration.");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml") && !name.startsWith("island_"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("[Migration] Keine alten Spieler-Vaults gefunden.");
            return;
        }

        plugin.getLogger().info("[Migration] Starte Migration von " + files.length + " Vault-Dateien...");

        for (File playerFile : files) {
            try {
                UUID playerUUID = UUID.fromString(playerFile.getName().replace(".yml", ""));
                Optional<Island> optIsland = bentoBox.getIslandsManager().getIslandAt(Objects.requireNonNull(Bukkit.getOfflinePlayer(playerUUID).getLocation()));

                if (optIsland.isEmpty()) {
                    plugin.getLogger().warning("[Migration] Keine Insel für Spieler " + playerUUID + " gefunden – überspringe.");
                    continue;
                }

                Island island = optIsland.get();
                String islandUUID = island.getUniqueId();

                File newFile = new File(folder, "island_" + islandUUID + ".yml");
                if (newFile.exists()) {
                    plugin.getLogger().info("[Migration] Insel-Datei existiert bereits – füge Inhalte zusammen.");
                    mergeVaultFiles(playerFile, newFile);
                } else {
                    playerFile.renameTo(newFile);
                    adjustFileStructure(newFile, playerUUID, islandUUID);
                }

                plugin.getLogger().info("[Migration] Vault von Spieler " + playerUUID + " → Insel " + islandUUID + " migriert.");

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Migration] Fehler bei Datei " + playerFile.getName(), e);
            }
        }

        plugin.getLogger().info("[Migration] Migration abgeschlossen.");
    }

    /**
     * Passt Struktur einer verschobenen Datei an das neue Insel-Format an.
     */
    private void adjustFileStructure(File file, UUID oldOwner, String islandId) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        cfg.set("Island.id", islandId.toString());
        cfg.set("Island.migrated_from_player", oldOwner.toString());

        // Optional: Umbenennung alter Materialkeys zu PermissionKeys
        if (cfg.contains("Inventory")) {
            Map<String, Object> oldMap = cfg.getConfigurationSection("Inventory").getValues(false);
            cfg.set("Inventory", null);

            for (Map.Entry<String, Object> entry : oldMap.entrySet()) {
                String oldKey = entry.getKey();
                String permissionKey = convertMaterialToPermissionKey(oldKey);
                cfg.set("Inventory." + permissionKey, entry.getValue());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[Migration] Fehler beim Speichern von " + file.getName(), e);
        }
    }

    /**
     * Fügt Inventar zweier Vaults (Spieler → Insel) zusammen.
     */
    private void mergeVaultFiles(File oldFile, File newFile) {
        FileConfiguration oldCfg = YamlConfiguration.loadConfiguration(oldFile);
        FileConfiguration newCfg = YamlConfiguration.loadConfiguration(newFile);

        if (oldCfg.contains("Inventory")) {
            for (String key : oldCfg.getConfigurationSection("Inventory").getKeys(false)) {
                int oldAmount = oldCfg.getInt("Inventory." + key);
                String permKey = convertMaterialToPermissionKey(key);

                int newAmount = newCfg.getInt("Inventory." + permKey, 0);
                newCfg.set("Inventory." + permKey, oldAmount + newAmount);
            }
        }

        try {
            newCfg.save(newFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[Migration] Fehler beim Zusammenführen von " + newFile.getName(), e);
        }

        // Alte Datei entfernen
        oldFile.delete();
    }

    /**
     * Wandelt alte Materialnamen in PermissionKeys um.
     * Beispiel: STONE → islandvault.item.stone
     */
    private String convertMaterialToPermissionKey(String oldKey) {
        return "islandvault.vault." + oldKey.toLowerCase(Locale.ROOT);
    }
}
