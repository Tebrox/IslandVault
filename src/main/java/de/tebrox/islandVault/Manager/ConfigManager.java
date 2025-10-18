package de.tebrox.islandVault.Manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> files = new HashMap<>();

    private final Map<String, Boolean> dirtyFlags = new HashMap<>();
    private final Map<String, BukkitRunnable> autosaveTasks = new HashMap<>();
    private final long AUTOSAVE_DELAY_TICKS = 20L * 120; // 5 Sekunden

    // Änderungsüberwachung
    private final Map<String, List<Long>> changeHistory = new HashMap<>();
    private static final int CHANGE_WINDOW_SECONDS = 10;
    private static final int CHANGE_THRESHOLD = 100;


    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    // ==========      GRUNDLEGENDES LADEN UND SPEICHERN     ======
    // ============================================================

    /**
     * Lädt eine Datei direkt beim Start (Startup-Load).
     */
    public void loadConfig(String name) {
        FileConfiguration cfg = loadInternal(name, true);
        configs.put(name, cfg);
    }

    /**
     * Registriert eine Datei für Lazy-Load (erst beim ersten Zugriff laden).
     */
    public void registerConfig(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte Datei " + name + ".yml nicht erstellen!");
                e.printStackTrace();
            }
        }
        files.put(name, file);
    }

    /**
     * Gibt eine Config zurück.
     * Falls sie noch nicht geladen ist, wird sie lazy geladen.
     */
    public FileConfiguration getConfig(String name) {
        if (!configs.containsKey(name)) {
            FileConfiguration cfg = loadInternal(name, false);
            if (cfg != null) {
                configs.put(name, cfg);
            }
        }
        return configs.get(name);
    }

    public boolean isConfigAvailable(String name) {
        return configs.containsKey(name);
    }

    /**
     * Lädt eine Datei (intern genutzt).
     */
    private FileConfiguration loadInternal(String name, boolean log) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte Datei " + name + ".yml nicht erstellen!");
                e.printStackTrace();
            }
        }

        // Wenn es ein Default-File im resources-Ordner gibt
        if (plugin.getResource(name + ".yml") != null) {
            try (InputStreamReader defConfigStream = new InputStreamReader(plugin.getResource(name + ".yml"))) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);

                // User-Werte übernehmen
                for (String key : defConfig.getKeys(true)) {
                    if (userConfig.contains(key)) {
                        defConfig.set(key, userConfig.get(key));
                    }
                }

                // Datei neu schreiben (alte Keys raus)
                defConfig.save(file);
                if (log) plugin.getLogger().info("[" + name + ".yml] geladen (mit Defaults).");
                files.put(name, file);
                return defConfig;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Ohne Defaults
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (log) plugin.getLogger().info("[" + name + ".yml] geladen (ohne Defaults).");
            files.put(name, file);
            return config;
        }
        return null;
    }

    /**
     * Speichert eine Config auf die Festplatte.
     * Falls sie noch nicht geladen war, wird sie vorher erstellt.
     */
    public void saveConfig(String name) {
        if (!configs.containsKey(name) || !files.containsKey(name)) {
            plugin.getLogger().warning("[ConfigManager] saveConfig() – Datei '" + name + "' nicht geladen.");
            return;
        }
        try {
            FileConfiguration config = configs.get(name);
            File file = files.get(name);
            config.save(file);
            dirtyFlags.put(name, false);
            plugin.getLogger().fine("[ConfigManager] Datei '" + name + "' gespeichert.");
        } catch (IOException e) {
            plugin.getLogger().severe("[ConfigManager] Fehler beim Speichern von '" + name + "': " + e.getMessage());
        }
    }


    /**
     * Speichert alle geladenen Configs.
     */
    public void saveAll() {
        for (String name : configs.keySet()) {
            saveConfig(name);
        }
    }

    /**
     * Entfernt eine Config aus dem Cache → wird beim nächsten Zugriff neu geladen.
     */
    public void reloadConfig(String name) {
        configs.remove(name);
    }

    /**
     * Entfernt alle Configs aus dem Cache → Lazy-Reload beim nächsten Zugriff.
     */
    public void reloadAll() {
        configs.clear();
    }

    public void markDirty(String name) {
        long now = System.currentTimeMillis();

        // Änderungs-Zeitpunkt aufzeichnen
        changeHistory.computeIfAbsent(name, k -> new ArrayList<>()).add(now);
        cleanupChangeHistory(name, now);

        // Wenn zu viele Änderungen in kurzer Zeit erfolgen → Warnung ausgeben
        if (changeHistory.get(name).size() >= CHANGE_THRESHOLD) {
            plugin.getLogger().warning("[ConfigManager] WARNUNG: Datei '" + name
                    + "' wurde in den letzten " + CHANGE_WINDOW_SECONDS + "s "
                    + changeHistory.get(name).size() + "× geändert! "
                    + "Prüfe, ob unnötige Saves oder Endlosschleifen auftreten.");
            // Zurücksetzen, damit nicht jede Sekunde erneut gewarnt wird
            changeHistory.get(name).clear();
        }

        dirtyFlags.put(name, true);

        // Bestehenden Autosave abbrechen, falls noch aktiv
        if (autosaveTasks.containsKey(name)) {
            autosaveTasks.get(name).cancel();
        }

        // Neuen Autosave planen
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (Boolean.TRUE.equals(dirtyFlags.get(name))) {
                    saveConfig(name);
                    dirtyFlags.put(name, false);
                }
                autosaveTasks.remove(name);
            }
        };
        task.runTaskLater(plugin, AUTOSAVE_DELAY_TICKS);
        autosaveTasks.put(name, task);
    }

    public void flushAllAutosaves() {
        plugin.getLogger().info("[ConfigManager] Speichere alle pending Autosaves...");
        for (String name : dirtyFlags.keySet()) {
            if (Boolean.TRUE.equals(dirtyFlags.get(name))) {
                saveConfig(name);
            }
        }
        dirtyFlags.clear();
    }

    private void cleanupChangeHistory(String name, long now) {
        List<Long> timestamps = changeHistory.get(name);
        if (timestamps == null) return;

        long cutoff = now - (CHANGE_WINDOW_SECONDS * 1000L);
        timestamps.removeIf(t -> t < cutoff);
    }

    public void clearChangeTracking() {
        changeHistory.clear();
    }

    public Map<String, Boolean> getDirtyFlags() {
        return Collections.unmodifiableMap(dirtyFlags);
    }

    public Map<String, BukkitRunnable> getAutosaveTasks() {
        return Collections.unmodifiableMap(autosaveTasks);
    }

    public Map<String, List<Long>> getChangeHistory() {
        return Collections.unmodifiableMap(changeHistory);
    }

    /**
     * Liefert die Anzahl der Änderungen innerhalb der letzten X Sekunden (z. B. 10 Sekunden).
     */
    public int getRecentChangeCount(String name) {
        if (!changeHistory.containsKey(name)) return 0;
        long now = System.currentTimeMillis();
        long cutoff = now - (CHANGE_WINDOW_SECONDS * 1000L);
        return (int) changeHistory.get(name).stream().filter(t -> t >= cutoff).count();
    }


    public int getChangeWindowSeconds() {
        return CHANGE_WINDOW_SECONDS;
    }


    // ============================================================
    // ==========       TEMPLATE-SYNCHRONISIERUNG           =======
    // ============================================================

    /**
     * Lädt eine Datei und synchronisiert sie mit einem Template aus resources/.
     * - Wenn Datei nicht existiert, wird sie erstellt.
     * - Keys aus dem Template werden ergänzt.
     * - Alte Keys, die im Template fehlen, werden entfernt.
     */
    public FileConfiguration loadWithTemplate(String name, String templatePath) {
        File file = new File(plugin.getDataFolder(), name + ".yml");

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte Datei " + name + ".yml nicht erstellen!");
                e.printStackTrace();
            }
        }

        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(file);

        if (plugin.getResource(templatePath) != null) {
            YamlConfiguration template = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(plugin.getResource(templatePath))
            );

            // User-Werte übernehmen, falls vorhanden
            for (String key : template.getKeys(true)) {
                if (userConfig.contains(key)) {
                    template.set(key, userConfig.get(key));
                }
            }

            // Datei neu schreiben (alte Keys, die im Template fehlen, werden entfernt)
            try {
                template.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }

            configs.put(name, template);
            files.put(name, file);
            plugin.getLogger().info("[" + name + ".yml] mit Template (" + templatePath + ") synchronisiert.");
            return template;
        }

        // Wenn kein Template existiert → normale Datei
        configs.put(name, userConfig);
        files.put(name, file);
        plugin.getLogger().info("[" + name + ".yml] ohne Template geladen.");
        return userConfig;
    }

    /**
     * Lädt eine Konfiguration nur von der Festplatte, ohne Defaults aus dem JAR zu übernehmen.
     * Wird für dynamische Dateien wie itemGroups.yml oder Vault-Dateien verwendet.
     */
    public void loadRawConfig(String name) {
        try {
            File file = new File(plugin.getDataFolder(), name + ".yml");
            if (!file.exists()) {
                plugin.getLogger().warning("[" + name + ".yml] nicht gefunden – es wird eine leere Datei erstellt.");
                file.createNewFile();
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            configs.put(name, config);
            files.put(name, file);

            plugin.getLogger().info("[" + name + ".yml] direkt von Festplatte geladen (ohne Defaults).");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[" + name + ".yml] konnte nicht geladen werden!", e);
        }
    }

    /**
     * Erstellt eine Konfigurationsdatei aus einem Template im Ressourcenordner,
     * falls sie noch nicht im Plugin-DataFolder existiert.
     *
     * @param name             Der Name (Pfad relativ zum Plugin-Folder, z. B. "chestData/world" oder "itemGroups")
     * @param templateResource Der Pfad des Templates im JAR (z. B. "chestData/template.yml" oder "itemGroups.yml")
     * @return true, wenn die Datei neu erstellt wurde, false wenn sie bereits existierte
     */
    public boolean createFromTemplateIfMissing(String name, String templateResource) {
        try {
            File file = new File(plugin.getDataFolder(), name + ".yml");

            if (file.exists()) {
                // Datei existiert bereits → nichts tun
                return false;
            }

            file.getParentFile().mkdirs();

            try (InputStream input = plugin.getResource(templateResource)) {
                if (input != null) {
                    Files.copy(input, file.toPath());
                    plugin.getLogger().info("[" + name + ".yml] aus Template (" + templateResource + ") erstellt.");
                } else {
                    // Kein Template gefunden → leere Datei anlegen
                    plugin.getLogger().warning("Kein Template gefunden für [" + name + ".yml]! Es wird eine leere Datei erstellt.");
                    file.createNewFile();
                }
            }

            // Datei als RawConfig registrieren
            loadRawConfig(name);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[" + name + ".yml] konnte nicht aus Template erstellt werden!", e);
            return false;
        }
    }

}