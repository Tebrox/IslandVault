package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class LanguageManager {

    private final Plugin plugin;
    private final File langFolder;
    private final Map<Locale, YamlConfiguration> languages = new HashMap<>();
    private final Map<UUID, Locale> playerLocales = new HashMap<>();
    private final Map<UUID, PlaceholderRegistry> playerPlaceholders = new HashMap<>();

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "lang");
        copyDefaultLangFiles();
        loadLanguages();
    }

    private void copyDefaultLangFiles() {
        if (!langFolder.exists()) langFolder.mkdirs();

        // Hier: feste Liste der Standard-Sprachdateien
        String[] defaultFiles = {"en.yml", "de.yml"};

        for (String fileName : defaultFiles) {
            File destFile = new File(langFolder, fileName);
            if (!destFile.exists()) {
                try (InputStream in = plugin.getResource("lang/" + fileName)) {
                    if (in == null) {
                        plugin.getLogger().warning("Sprachdatei nicht gefunden in resources: lang/" + fileName);
                        continue;
                    }
                    Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Kopiere Standard-Sprachdatei: " + fileName);
                } catch (Exception e) {
                    plugin.getLogger().severe("Fehler beim Kopieren der Sprachdatei " + fileName);
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadLanguages() {
        if (!langFolder.exists()) langFolder.mkdirs();

        languages.clear();

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName().replace(".yml", "");
            Locale locale = Locale.forLanguageTag(name.replace("_", "-"));
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            languages.put(locale, config);
        }
    }

    public void setLocale(Player player, Locale locale) {
        playerLocales.put(player.getUniqueId(), locale);
    }

    public Locale getLocale(Player player) {
        return playerLocales.getOrDefault(player.getUniqueId(), Locale.ENGLISH);
    }

    public PlaceholderRegistry getPlaceholders(Player player) {
        return playerPlaceholders.computeIfAbsent(player.getUniqueId(), id -> new PlaceholderRegistry());
    }

    public String translate(Player player, String path) {
        return translate(player, path, Map.of(), true);
    }

    public String translate(Player player, String path, Map<String, String> extraPlaceholders, boolean useGlobal) {
        Locale locale = getLocale(player);
        YamlConfiguration config = languages.getOrDefault(locale, languages.get(Locale.ENGLISH));
        String raw = config.getString(path);
        if (raw == null) return "§cMissing lang: " + path;

        if (useGlobal) {
            Map<String, String> global = getPlaceholders(player).getAll();
            for (var entry : global.entrySet()) {
                raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        for (var entry : extraPlaceholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public List<String> translateList(Player player, String path) {
        return translateList(player, path, Map.of(), true);
    }

    public List<String> translateList(Player player, String path, Map<String, String> extraPlaceholders, boolean useGlobal) {
        Locale locale = getLocale(player);
        YamlConfiguration config = languages.getOrDefault(locale, languages.get(Locale.ENGLISH));
        List<String> list = config.getStringList(path);
        if (list.isEmpty()) {
            String single = config.getString(path);
            if (single != null) list = List.of(single);
        }
        List<String> result = new ArrayList<>();
        for (String line : list) {
            if (useGlobal) {
                Map<String, String> global = getPlaceholders(player).getAll();
                for (var entry : global.entrySet()) {
                    line = line.replace("%" + entry.getKey() + "%", entry.getValue());
                }
            }
            for (var entry : extraPlaceholders.entrySet()) {
                line = line.replace("%" + entry.getKey() + "%", entry.getValue());
            }
            result.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return result;
    }

    public void reloadLanguages(CommandSender sender) {
        loadLanguages();
        IslandVault.getPlugin().getLogger().info("Sprachdateien neu geladen.");
        if(sender != null) {
            sender.sendMessage("Sprachdateien neu geladen.");
        }
    }

    public static class PlaceholderRegistry {
        private final Map<String, String> placeholders = new HashMap<>();

        public void set(String key, String value) {
            placeholders.put(key, value);
        }

        public void setAll(Map<String, String> values) {
            placeholders.putAll(values);
        }

        public void remove(String key) {
            placeholders.remove(key);
        }

        public Map<String, String> getAll() {
            return placeholders;
        }

        public void clear() {
            placeholders.clear();
        }
    }
}
