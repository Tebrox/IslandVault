package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.Menu.VaultMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages synchronization of open vault menus across multiple viewers of the same island vault.
 * <p>
 * Keeps track of which players have vault menus open and allows updates (e.g., after a vault change)
 * to be propagated to all open menus.
 */
public class VaultSyncManager {
    private Plugin plugin;
    private final Map<String, Set<VaultMenu>> openViewers = new ConcurrentHashMap<>();

    /**
     * Constructs a new VaultSyncManager for the given plugin instance.
     *
     * @param plugin the plugin instance
     */
    public VaultSyncManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a player as a viewer of a vault for a specific island.
     * This should be called when a player opens a VaultMenu.
     *
     * @param islandId the ID of the island
     * @param player the player who opened the menu
     * @param menu the VaultMenu instance
     */
    public void registerViewer(String islandId, Player player, VaultMenu menu) {
        openViewers.computeIfAbsent(islandId, k -> ConcurrentHashMap.newKeySet()).add(menu);
    }

    /**
     * Unregisters a player as a viewer of a vault.
     * This should be called when the player closes the menu or disconnects.
     *
     * @param islandId the ID of the island
     * @param player the player to unregister
     */
    public void unregisterViewer(String islandId, Player player) {
        Set<VaultMenu> menus = openViewers.get(islandId);
        if (menus != null) {
            menus.removeIf(menu -> menu.getViewer().getUniqueId().equals(player.getUniqueId()));
            if (menus.isEmpty()) {
                openViewers.remove(islandId);
            }
        }
    }

    /**
     * Refreshes the inventory view for all viewers of a specific island vault.
     * This should be called when the contents of a vault are changed to reflect the new state in all open menus.
     *
     * @param islandId the ID of the island whose viewers should be refreshed
     */
    public void refreshViewers(String islandId) {
        Set<VaultMenu> menus = openViewers.get(islandId);
        if (menus != null) {
            for (VaultMenu menu : menus) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    menu.refreshData();
                    menu.getViewer().updateInventory();
                });
            }
        }
    }
}