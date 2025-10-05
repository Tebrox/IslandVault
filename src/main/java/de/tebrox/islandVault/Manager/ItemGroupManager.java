package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Utils.PermissionUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;

/**
 * Lädt Item-Gruppen aus itemGroups.yml (über ConfigManager).
 * Überschreibt keine Daten und nutzt kein Template.
 */
public final class ItemGroupManager {

    private static final Map<String, List<String>> permissionGroups = new HashMap<>();

    private static Plugin plugin;
    private static ConfigManager configManager;

    private static final String FILE_NAME = "itemGroups"; // plugins/IslandVault/itemGroups.yml

    private ItemGroupManager() {}

    /**
     * Initialisiert den Manager. Erwartet, dass die Datei itemGroups.yml existiert.
     * Wird nicht überschrieben – liest nur vorhandene Daten ein.
     */
    public static void init(Plugin pluginInstance, ConfigManager manager) {
        plugin = pluginInstance;
        configManager = manager;

        // Nur laden, keine Templates mehr anwenden
        configManager.createFromTemplateIfMissing("itemGroups", "itemGroups_template.yml");
        configManager.loadRawConfig(FILE_NAME);
        loadGroups();
    }

    /**
     * Liest Gruppen aus itemGroups.yml.
     */
    private static void loadGroups() {
        permissionGroups.clear();

        FileConfiguration cfg = configManager.getConfig(FILE_NAME);
        ConfigurationSection section = cfg.getConfigurationSection("groups");

        if (section == null || section.getKeys(false).isEmpty()) {
            plugin.getLogger().warning("[ItemGroupManager] Keine Gruppen in itemGroups.yml gefunden!");
            return;
        }

        plugin.getLogger().info("[ItemGroupManager] Lade Item-Gruppen aus itemGroups.yml ...");

        for (String groupName : section.getKeys(false)) {
            List<String> items = section.getStringList(groupName);
            permissionGroups.put(groupName, items);

            PermissionUtils.registerPermission(
                    Permissions.GROUPS.getLabel() + groupName,
                    "Erlaubt Zugriff auf Item-Gruppe '" + groupName + "'",
                    PermissionDefault.FALSE
            );

            plugin.getLogger().info("[ItemGroupManager] Gruppe '" + groupName + "' mit " + items.size() + " Items geladen.");
        }
    }

    public static Map<String, List<String>> getPermissionGroups() {
        return permissionGroups;
    }

    public static List<String> getGroupItems(String group) {
        return permissionGroups.getOrDefault(group, Collections.emptyList());
    }

    public static List<String> findGroupsWithMaterial(String materialToFind) {
        List<String> result = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : permissionGroups.entrySet()) {
            if (entry.getValue().stream().anyMatch(m -> m.equalsIgnoreCase(materialToFind))) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    public static void reloadGroups() {
        configManager.reloadConfig(FILE_NAME);
        loadGroups();
        plugin.getLogger().info("[ItemGroupManager] itemGroups.yml neu geladen.");
    }
}
