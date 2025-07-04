package de.tebrox.islandVault.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class PermissionGroupConverter {

    private final File configFile;
    private final File itemGroupFolder;
    private final YamlConfiguration config;
    private final Gson gson;

    public PermissionGroupConverter(JavaPlugin plugin) {
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.itemGroupFolder = new File(plugin.getDataFolder(), "ItemGroups");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void convert() {
        if (!config.contains("permission_groups")) {
            return; // nichts zu tun
        }

        ConfigurationSection groupsSection = config.getConfigurationSection("permission_groups");
        if (groupsSection == null) return;

        if (!itemGroupFolder.exists()) {
            itemGroupFolder.mkdirs();
        }

        for (String group : groupsSection.getKeys(false)) {
            List<String> items = config.getStringList("permission_groups." + group);
            if (items.isEmpty()) continue;

            File jsonFile = new File(itemGroupFolder, group.toLowerCase() + ".json");
            try (FileWriter writer = new FileWriter(jsonFile)) {
                JsonObject json = new JsonObject();
                json.addProperty("group", group);

                JsonArray itemArray = new JsonArray();
                for (String item : items) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("material", item);
                    itemArray.add(itemObj);
                }

                json.add("items", itemArray);
                gson.toJson(json, writer);

                PluginLogger.info("[MenuAPI] Gruppe '" + group + "' konvertiert.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Config bereinigen
        config.set("permission_groups", null);
        try {
            config.save(configFile);
            PluginLogger.info("[MenuAPI] Alte Gruppen aus config.yml entfernt.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}