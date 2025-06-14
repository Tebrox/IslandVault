package de.tebrox.islandVault.Manager;

import com.google.gson.*;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Utils.PermissionUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class ItemGroupManager {

    private static final Map<String, List<String>> permissionGroups = new HashMap<>();

    private static File itemGroupFolder;
    private static Gson gson;
    private static Plugin plugin;

    private ItemGroupManager() {
    }

    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
        itemGroupFolder = new File(plugin.getDataFolder(), "ItemGroups");
        gson = new GsonBuilder().setPrettyPrinting().create();

        if (!itemGroupFolder.exists()) itemGroupFolder.mkdirs();
        loadGroups();
    }

    private static void loadGroups() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("permission_groups");
        if (section != null && !section.getKeys(false).isEmpty()) {
            plugin.getLogger().info("Old permission_groups config found, converting...");
            convertOldConfig(section);
            plugin.getConfig().set("permission_groups", null);
            plugin.saveConfig();
        }
        loadGroupsFromJson();
    }

    private static void convertOldConfig(ConfigurationSection section) {
        for (String groupName : section.getKeys(false)) {
            List<String> items = plugin.getConfig().getStringList("permission_groups." + groupName);
            permissionGroups.put(groupName, items);

            File groupFile = new File(itemGroupFolder, groupName + ".json");
            try (FileWriter writer = new FileWriter(groupFile)) {
                JsonObject json = new JsonObject();
                json.addProperty("group", groupName);

                JsonArray itemArray = new JsonArray();
                for (String item : items) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("material", item);
                    itemArray.add(itemObj);
                }

                json.add("items", itemArray);
                gson.toJson(json, writer);

                plugin.getLogger().info("Converted group " + groupName + " to JSON file.");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to write group JSON file for " + groupName, e);
            }
        }
    }

    private static void loadGroupsFromJson() {
        permissionGroups.clear();

        File[] files = itemGroupFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                String groupName = json.get("group").getAsString();
                JsonArray itemsArray = json.getAsJsonArray("items");

                List<String> items = new ArrayList<>();
                for (int i = 0; i < itemsArray.size(); i++) {
                    JsonObject itemObj = itemsArray.get(i).getAsJsonObject();
                    String material = itemObj.get("material").getAsString();
                    items.add(material);
                }

                permissionGroups.put(groupName, items);
                PermissionUtils.registerPermission(Permissions.GROUPS + groupName, "Group " + groupName, PermissionDefault.FALSE);
                plugin.getLogger().info("Loaded group " + groupName + " with " + items.size() + " items.");
            } catch (IOException | IllegalStateException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load group JSON file " + file.getName(), e);
            }
        }
    }

    public static Map<String, List<String>> getPermissionGroups() {
        return permissionGroups;
    }

    public static void saveGroup(String groupName, List<String> items) {
        File file = new File(itemGroupFolder, groupName.toLowerCase() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            JsonObject json = new JsonObject();
            json.addProperty("group", groupName);

            JsonArray itemArray = new JsonArray();
            for (String item : items) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("material", item);
                itemArray.add(itemObj);
            }

            json.add("items", itemArray);
            gson.toJson(json, writer);
            permissionGroups.put(groupName, items);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save group: " + groupName, e);
        }
    }

    public static void deleteGroup(String groupName) {
        permissionGroups.remove(groupName);
        File file = new File(itemGroupFolder, groupName.toLowerCase() + ".json");
        if (file.exists()) {
            if (file.delete()) {
                plugin.getLogger().info("Deleted group file: " + groupName);
            } else {
                plugin.getLogger().warning("Failed to delete group file: " + groupName);
            }
        }
    }

    public static void reloadGroups() {
        loadGroups();
    }

    public static List<String> findGroupsWithMaterial(String materialToFind) {
        List<String> result = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : permissionGroups.entrySet()) {
            List<String> values = entry.getValue();
            if (values.stream().anyMatch(m -> m.equalsIgnoreCase(materialToFind))) {
                result.add(entry.getKey());
            }
        }

        return result;
    }
}