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


/**
 * Manages language files and translations for different player locales.
 * <p>
 * This class handles loading language YAML files from a "lang" folder,
 * copying default language files from plugin resources if missing,
 * caching loaded languages, and performing placeholder replacements.
 * <p>
 * Supports per-player placeholder registries to inject dynamic values
 * into translated strings. Also supports translation of single strings
 * and lists of strings.
 */
public class LanguageManager {

    private final Plugin plugin;
    private final File langFolder;
    private final Map<String, YamlConfiguration> languageCache = new HashMap<>();
    private final Map<UUID, PlaceholderRegistry> playerPlaceholders = new HashMap<>();

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\w+)(:([^%]+))?%");

    /**
     * Creates a new LanguageManager instance and loads languages.
     *
     * @param plugin the plugin instance used to locate resources and plugin folder
     */
    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "lang");
        copyDefaultLangFiles();
        loadLanguages();
    }

    /**
     * Copies default language files from the plugin's resources to the lang folder if not present.
     * Supports a fixed list of default files ("en.yml", "de.yml").
     */
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

    /**
     * Loads all language YAML files from the lang folder into memory.
     * Clears any previously loaded languages.
     * Logs warnings if no language files are found or if any file fails to load.
     */
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

    /**
     * Gets the PlaceholderRegistry for a given player.
     * If none exists yet, a new one will be created and cached.
     *
     * @param player the player whose placeholder registry is requested
     * @return the player's PlaceholderRegistry
     */
    public PlaceholderRegistry getPlaceholders(Player player) {
        return playerPlaceholders.computeIfAbsent(player.getUniqueId(), id -> new PlaceholderRegistry());
    }

    /**
     * Replaces placeholders in the input text using the provided placeholder map.
     * Placeholders have the form %key% or %key:defaultValue% where defaultValue is used if the key is missing.
     *
     * @param text         the input text containing placeholders
     * @param placeholders a map of placeholder keys to replacement strings
     * @return the text with placeholders replaced
     */
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

    /**
     * Gets the language key for a player based on their locale.
     * Extracts the language part before underscore and converts to lowercase.
     *
     * @param player the player whose language key is requested
     * @return the language key (e.g. "en", "de")
     */
    public String getPlayerLanguageKey(Player player) {
        String locale = player.getLocale();
        return locale.split("_")[0].toLowerCase(Locale.ROOT);
    }

    /**
     * Translates a message path for the given player using global placeholders.
     *
     * @param player the player for whom to translate
     * @param path   the path/key in the language file
     * @return the translated and colorized string, or error message if missing
     */
    public String translate(Player player, String path) {
        return translate(player, path, Map.of(), true);
    }

    /**
     * Translates a message path for the given player, optionally using additional placeholders and global placeholders.
     *
     * @param player            the player for whom to translate
     * @param path              the path/key in the language file
     * @param extraPlaceholders extra placeholders to merge in addition to global ones
     * @param useGlobal         whether to include the global per-player placeholders
     * @return the translated and colorized string, or error message if missing
     */
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

    /**
     * Translates a list of strings from the language file for the given player.
     *
     * @param player the player for whom to translate
     * @param path   the path/key in the language file
     * @return the list of translated and colorized strings
     */
    public List<String> translateList(Player player, String path) {
        return translateList(player, path, Map.of(), true);
    }

    /**
     * Translates a list of strings from the language file for the given player,
     * with extra placeholders and optional global placeholders.
     * If the list is empty but a single string exists at path, returns a single-item list.
     *
     * @param player            the player for whom to translate
     * @param path              the path/key in the language file
     * @param extraPlaceholders extra placeholders to merge in addition to global ones
     * @param useGlobal         whether to include the global per-player placeholders
     * @return the list of translated and colorized strings
     */
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

    /**
     * Reloads all language files from disk and notifies a command sender if provided.
     *
     * @param sender the CommandSender to notify, or null if none
     */
    public void reloadLanguages(CommandSender sender) {
        loadLanguages();
        IslandVault.getPlugin().getLogger().info("Sprachdateien neu geladen.");
        if(sender != null) {
            sender.sendMessage("Sprachdateien neu geladen.");
        }
    }

    /**
     * Stores and manages placeholder key-value pairs for translation replacement.
     * Each player has their own PlaceholderRegistry to hold personalized placeholders.
     */
    public static class PlaceholderRegistry {
        private final Map<String, String> placeholders = new HashMap<>();

        /**
         * Sets a placeholder key to a specific value.
         *
         * @param key   the placeholder key (without percent signs)
         * @param value the replacement value for this placeholder
         */
        public void set(String key, String value) {
            placeholders.put(key, value);
        }

        /**
         * Adds or updates multiple placeholders at once.
         *
         * @param values a map containing placeholder keys and their replacement values
         */
        public void setAll(Map<String, String> values) {
            placeholders.putAll(values);
        }

        /**
         * Removes a placeholder by its key.
         *
         * @param key the placeholder key to remove
         */
        public void remove(String key) {
            placeholders.remove(key);
        }

        /**
         * Returns an unmodifiable view of all stored placeholders.
         *
         * @return a map of all placeholder keys and their values
         */
        public Map<String, String> getAll() {
            return placeholders;
        }

        /**
         * Clears all stored placeholders.
         */
        public void clear() {
            placeholders.clear();
        }
    }
}
