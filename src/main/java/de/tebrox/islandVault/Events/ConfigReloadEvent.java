package de.tebrox.islandVault.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.List;

/**
 * Wird ausgelöst, wenn eine oder mehrere Konfigurationsdateien neu geladen wurden.
 * Beispiel: ConfigManager.loadConfig(), loadRawConfig() oder reloadAll().
 */
public class ConfigReloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final List<String> reloadedConfigs;

    /**
     * @param reloadedConfigs Liste der neu geladenen Konfigurationsdateien (z. B. ["config.yml", "itemGroups.yml"])
     */
    public ConfigReloadEvent(List<String> reloadedConfigs) {
        super(true); // async=false → im Bukkit Main Thread; true wäre async
        this.reloadedConfigs = reloadedConfigs != null
                ? Collections.unmodifiableList(reloadedConfigs)
                : Collections.emptyList();
    }

    public List<String> getReloadedConfigs() {
        return reloadedConfigs;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
