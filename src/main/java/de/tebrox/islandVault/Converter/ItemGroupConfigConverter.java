package de.tebrox.islandVault.Converter;

import de.tebrox.islandVault.Enums.Prefix;
import de.tebrox.islandVault.Manager.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Level;

/**
 * Konvertiert alte permission_groups aus config.yml in das neue itemGroups.yml-Format.
 * Alte Daten bleiben erhalten, nichts wird überschrieben.
 * Eine Warnung wird nur einmalig eingetragen – danach wird der Converter übersprungen.
 */
public final class ItemGroupConfigConverter {

    private ItemGroupConfigConverter() {}

    public static void convertOldGroups(Plugin plugin, ConfigManager configManager) {
        try {
            FileConfiguration oldConfig = plugin.getConfig();

            // 1️⃣ Prüfen, ob bereits eine Warnung existiert → Migration schon erfolgt
            if (oldConfig.isSet("DEPRECATED_WARNING")) {
                plugin.getLogger().fine(Prefix.ITEMGROUPCONVERTER + " Migration bereits abgeschlossen – Warnung existiert, kein weiterer Vorgang nötig.");
                return;
            }

            // 2️⃣ Prüfen, ob überhaupt alte permission_groups vorhanden sind
            ConfigurationSection section = oldConfig.getConfigurationSection("permission_groups");
            if (section == null || section.getKeys(false).isEmpty()) {
                plugin.getLogger().fine("[ItemGroupConverter] Keine alten permission_groups in config.yml gefunden – keine Konvertierung nötig.");
                return;
            }

            plugin.getLogger().warning("[ItemGroupConverter] Alte 'permission_groups' in config.yml gefunden!");
            plugin.getLogger().warning("Diese Struktur ist veraltet und wird in zukünftigen Versionen entfernt.");
            plugin.getLogger().warning("Die Gruppen werden nach itemGroups.yml kopiert, bleiben aber vorerst bestehen.");

            // 3️⃣ itemGroups.yml laden (ohne Template – nichts überschreiben)
            configManager.loadRawConfig("itemGroups");
            FileConfiguration newFile = configManager.getConfig("itemGroups");

            if (newFile.getConfigurationSection("groups") == null) {
                newFile.createSection("groups");
            }
            ConfigurationSection groupsSection = newFile.getConfigurationSection("groups");

            int transferred = 0;
            for (String groupName : section.getKeys(false)) {
                List<String> items = section.getStringList(groupName);
                if (items.isEmpty()) {
                    plugin.getLogger().warning("[ItemGroupConverter] Gruppe '" + groupName + "' enthält keine Items – übersprungen.");
                    continue;
                }

                if (!groupsSection.isSet(groupName)) {
                    groupsSection.set(groupName, items);
                    plugin.getLogger().info("[ItemGroupConverter] Neue Gruppe '" + groupName + "' mit " + items.size() + " Items hinzugefügt.");
                    transferred++;
                } else {
                    plugin.getLogger().info("[ItemGroupConverter] Gruppe '" + groupName + "' existiert bereits in itemGroups.yml – nicht überschrieben.");
                }
            }

            if (transferred > 0) {
                configManager.saveConfig("itemGroups");
                plugin.getLogger().info("[ItemGroupConverter] " + transferred + " Gruppen erfolgreich nach itemGroups.yml kopiert.");
            } else {
                plugin.getLogger().info("[ItemGroupConverter] Keine neuen Gruppen kopiert – itemGroups.yml ist bereits vollständig.");
            }

            // 4️⃣ Dauerhafte Warnung in config.yml einfügen
            String warningText =
                    "ACHTUNG: Der Abschnitt 'permission_groups' ist veraltet und wird in zukünftigen Versionen entfernt!\n" +
                            "Bitte migriere deine Gruppen nach itemGroups.yml!\n" +
                            "Die aktuellen Daten wurden bereits automatisch kopiert.";

            oldConfig.set("DEPRECATED_WARNING", warningText);
            plugin.saveConfig();

            plugin.getLogger().warning("[ItemGroupConverter] WARNUNG in config.yml eingetragen – Admins sollten die Migration prüfen.");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[ItemGroupConverter] Fehler beim Konvertieren der alten permission_groups!", e);
        }
    }
}
