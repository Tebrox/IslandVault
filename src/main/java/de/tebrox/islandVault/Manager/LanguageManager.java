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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager {

    private final Plugin plugin;
    private final File langFolder;
    private final Map<String, YamlConfiguration> languageCache = new HashMap<>();
    private final Map<UUID, PlaceholderRegistry> playerPlaceholders = new HashMap<>();

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\w+)(:([^%]+))?%");

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

        languageCache.clear();

        File[] files = langFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("Keine Sprachdateien im lang-Ordner gefunden: " + langFolder.getAbsolutePath());
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String langKey = file.getName().replace(".yml", "").toLowerCase(Locale.ROOT);
                languageCache.put(langKey, config);
                plugin.getLogger().info("Sprachdatei geladen: " + file.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Laden der Sprachdatei: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    public PlaceholderRegistry getPlaceholders(Player player) {
        return playerPlaceholders.computeIfAbsent(player.getUniqueId(), id -> new PlaceholderRegistry());
    }

    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);            // z.B. "amount"
            String defaultValue = matcher.group(3);   // z.B. "64" oder null

            String replacement = placeholders.get(key);
            if (replacement == null) {
                replacement = defaultValue != null ? defaultValue : "";
            }

            // Escape $ und \ für Matcher.appendReplacement
            replacement = Matcher.quoteReplacement(replacement);

            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public String getPlayerLanguageKey(Player player) {
        String locale = player.getLocale();
        return locale.split("_")[0].toLowerCase(Locale.ROOT);
    }

    public String translate(Player player, String path) {
        return translate(player, path, Map.of(), true);
    }

    public String translate(Player player, String path, Map<String, String> extraPlaceholders, boolean useGlobal) {
        String locale = getPlayerLanguageKey(player);
        YamlConfiguration config = languageCache.getOrDefault(locale, languageCache.get(Locale.ENGLISH));
        String raw = config.getString(path);
        if (raw == null) return "§cMissing lang: " + path;

        Map<String, String> combined = new HashMap<>();
        if (useGlobal) combined.putAll(getPlaceholders(player).getAll());
        combined.putAll(extraPlaceholders);

        raw = replacePlaceholders(raw, combined);

        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public List<String> translateList(Player player, String path) {
        return translateList(player, path, Map.of(), true);
    }

    public List<String> translateList(Player player, String path, Map<String, String> extraPlaceholders, boolean useGlobal) {
        String locale = getPlayerLanguageKey(player);
        YamlConfiguration config = languageCache.getOrDefault(locale, languageCache.get(Locale.ENGLISH));
        List<String> list = config.getStringList(path);
        if (list.isEmpty()) {
            String single = config.getString(path);
            if (single != null) list = List.of(single);
        }
        List<String> result = new ArrayList<>();
        Map<String, String> combined = new HashMap<>();

        if (useGlobal) combined.putAll(getPlaceholders(player).getAll());
        combined.putAll(extraPlaceholders);

        for (String line : list) {
            line = replacePlaceholders(line, combined);
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
