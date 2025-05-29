package de.tebrox.islandVault.Utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginDependencyChecker {
    private static final List<String> missingRequired = new ArrayList<>();
    private static final List<String> missingOptional = new ArrayList<>();

    // Pflicht-Plugins
    private static String[] requiredPlugins = {};

    // Optionale Plugins
    private static String[] optionalPlugins = {};

    public static void setup(String[] requiredPlugins, String[] optionalPlugins) {
        PluginDependencyChecker.requiredPlugins = requiredPlugins;
        PluginDependencyChecker.optionalPlugins = optionalPlugins;
    }

    /**
     * Führt die Prüfung aller Abhängigkeiten durch.
     */
    public static void checkDependencies() {
        missingRequired.clear();
        missingOptional.clear();

        // Pflicht-Plugins prüfen
        for (String pluginName : requiredPlugins) {
            if (!isPluginEnabled(pluginName)) {
                missingRequired.add(pluginName);
            }
        }

        // Optionale Plugins prüfen
        for (String pluginName : optionalPlugins) {
            if (!isPluginEnabled(pluginName)) {
                missingOptional.add(pluginName);
            }
        }
    }

    /**
     * Prüft, ob ein Plugin geladen und aktiviert ist.
     */
    private static boolean isPluginEnabled(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Gibt true zurück, wenn alle Pflicht-Plugins geladen sind.
     */
    public static boolean allRequiredPresent() {
        return missingRequired.isEmpty();
    }

    public static List<String> getMissingRequiredPlugins() {
        return Collections.unmodifiableList(missingRequired);
    }

    public static List<String> getMissingOptionalPlugins() {
        return Collections.unmodifiableList(missingOptional);
    }

    public static String getFormattedList(List<String> plugins) {
        return String.join(", ", plugins);
    }
}
