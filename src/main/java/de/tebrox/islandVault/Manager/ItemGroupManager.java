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

    private static Plugin plugin;

    private ItemGroupManager() {
    }

    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
        loadGroups();
    }

    private static void loadGroups() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("permission_groups");
        if (section != null && !section.getKeys(false).isEmpty()) {
            plugin.getLogger().info("Loading permission_groups from config.yml");
            loadFromOldConfig(section);
        }
    }

    private static void loadFromOldConfig(ConfigurationSection section) {
        permissionGroups.clear();

        for (String groupName : section.getKeys(false)) {
            List<String> items = section.getStringList(groupName);
            permissionGroups.put(groupName, items);

            PermissionUtils.registerPermission(Permissions.GROUPS.getLabel() + groupName, "Group " + groupName, PermissionDefault.FALSE);
            plugin.getLogger().info("Loaded group " + groupName + " with " + items.size() + " items from config.yml.");
        }
    }

    public static Map<String, List<String>> getPermissionGroups() {
        return permissionGroups;
    }

    public static void reloadGroups() {
        loadGroups();
    }

    public static List<String> getGroupItems(String group) {
        return permissionGroups.getOrDefault(group, new ArrayList<>());
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