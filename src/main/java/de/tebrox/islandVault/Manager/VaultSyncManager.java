package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.Menu.VaultMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VaultSyncManager {
    private Plugin plugin;
    private final Map<String, Set<VaultMenu>> openViewers = new ConcurrentHashMap<>();

    public VaultSyncManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void registerViewer(String islandId, Player player, VaultMenu menu) {
        openViewers.computeIfAbsent(islandId, k -> ConcurrentHashMap.newKeySet()).add(menu);
    }

    public void unregisterViewer(String islandId, Player player) {
        Set<VaultMenu> menus = openViewers.get(islandId);
        if (menus != null) {
            menus.removeIf(menu -> menu.getViewer().getUniqueId().equals(player.getUniqueId()));
            if (menus.isEmpty()) {
                openViewers.remove(islandId);
            }
        }
    }

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