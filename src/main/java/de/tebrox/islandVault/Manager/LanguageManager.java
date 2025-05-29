package de.tebrox.islandVault.Manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager {
    private final JavaPlugin plugin;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final String fallbackLanguage = "en";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%:]+)(?::([^%]+))?%");

    private final File langFolder;

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
        languages.clear();

        if (!langFolder.exists() || !langFolder.isDirectory()) {
            plugin.getLogger().warning("Der Sprachordner existiert nicht: " + langFolder.getAbsolutePath());
            return;
        }

        File[] files = langFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("Keine Sprachdateien im lang-Ordner gefunden: " + langFolder.getAbsolutePath());
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String langKey = file.getName().replace(".yml", "").toLowerCase(Locale.ROOT);
                languages.put(langKey, config);
                plugin.getLogger().info("Sprachdatei geladen: " + file.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Laden der Sprachdatei: " + file.getName());
                e.printStackTrace();
            }
        }

        if (!languages.containsKey(fallbackLanguage)) {
            plugin.getLogger().warning("Fallback-Sprache '" + fallbackLanguage + "' nicht gefunden! Bitte en.yml bereitstellen.");
        }
    }

    public void reloadLanguages() {
        loadLanguages();
        Bukkit.getLogger().info("[IslandVault] Sprachdateien neu geladen.");
    }

    private String getPlayerLanguageKey(Player player) {
        String locale = player.getLocale();
        return locale.split("_")[0].toLowerCase(Locale.ROOT);
    }

    public String translate(Player player, String key) {
        return translate(player, key, Collections.emptyMap());
    }

    public String translate(Player player, String key, Map<String, String> placeholders) {
        String lang = getPlayerLanguageKey(player);
        YamlConfiguration config = languages.getOrDefault(lang, languages.get(fallbackLanguage));
        if (config == null) return key;

        String value = config.getString(key);
        if (value == null) {
            value = languages.get(fallbackLanguage).getString(key, key);
        }

        return ChatColor.translateAlternateColorCodes('&', applyPlaceholders(value,placeholders));
    }

    public List<String> translateList(Player player, String key) {
        return translateList(player, key, Collections.emptyMap());
    }

    public List<String> translateList(Player player, String key, Map<String, String> placeholders) {
        String lang = getPlayerLanguageKey(player);
        YamlConfiguration config = languages.getOrDefault(lang, languages.get(fallbackLanguage));
        if (config == null) return Collections.singletonList(key);

        List<String> lines = config.getStringList(key);
        if (lines.isEmpty()) {
            lines = languages.get(fallbackLanguage).getStringList(key);
            if (lines.isEmpty()) return Collections.singletonList(key);
        }

        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(applyPlaceholders(ChatColor.translateAlternateColorCodes('&', line), placeholders));
        }
        return result;
    }

    private String applyPlaceholders(String input, Map<String, String> placeholders) {
        if (input == null) return null;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String defaultValue = matcher.group(2);
            String replacement = placeholders.getOrDefault(key, defaultValue != null ? defaultValue : matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Component getLocalizedItemName(Material material) {
        String translationKey = "item.minecraft." + material.toString().toLowerCase(); // z. B. "item.minecraft.diamond_sword"
        return Component.translatable(translationKey);
    }
}
