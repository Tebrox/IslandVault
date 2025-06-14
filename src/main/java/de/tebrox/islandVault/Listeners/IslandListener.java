package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.IslandTracker;
import de.tebrox.islandVault.Manager.LanguageManager;
import de.tebrox.islandVault.Manager.MenuManager;
import de.tebrox.islandVault.Utils.IslandUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.events.island.IslandEnterEvent;
import world.bentobox.bentobox.api.events.island.IslandExitEvent;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

public class IslandListener implements Listener {

    private final Addon addon;

    public IslandListener() {
        this.addon = BentoBox.getInstance().getAddonsManager().getAddonByName("AOneBlock").orElse(null);
    }

    @EventHandler
    public void onIslandEnter(IslandEnterEvent event) {
        UUID playerUUID = event.getPlayerUUID();
        Player player = Bukkit.getPlayer(playerUUID);

        // Hole die Insel an der Position des Spielers (z. B. Spawnpunkt)
        Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
        if (island != null) {
            if(IslandTracker.isIslandEmpty(island.getUniqueId())) {
                IslandVault.getVaultManager().loadOrCreateVault(island.getUniqueId(), Bukkit.getOfflinePlayer(island.getOwner()).getName());
            }
            IslandTracker.setPlayerIsland(player.getUniqueId(), island);
        } else {
            IslandTracker.removePlayerIsland(player.getUniqueId());
        }
    }

    @EventHandler
    public void onIslandExit(IslandExitEvent event) {
        UUID playerUUID = event.getPlayerUUID();
        Player player = Bukkit.getPlayer(playerUUID);
        Island island = IslandTracker.getPlayerIsland(player.getUniqueId());
        if (island != null) {
            IslandTracker.removePlayerIsland(player.getUniqueId());

            if (IslandTracker.isIslandEmpty(island.getUniqueId())) {
                IslandVault.getVaultManager().saveVaultAsync(island.getUniqueId(), Bukkit.getOfflinePlayer(island.getOwner()).getName());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        MenuManager.addPlayerMenuUtilityMap(player);
        LanguageManager.PlaceholderRegistry placeholderRegistry = IslandVault.getLanguageManager().getPlaceholders(player);
        placeholderRegistry.set("player", player.getName());

        Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
        if (island != null) {
            if(IslandTracker.isIslandEmpty(island.getUniqueId())) {
                IslandVault.getVaultManager().loadOrCreateVault(island.getUniqueId(), Bukkit.getOfflinePlayer(island.getOwner()).getName());
            }
            IslandTracker.setPlayerIsland(player.getUniqueId(), island);
        } else {
            IslandTracker.removePlayerIsland(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        MenuManager.removeFromPlayerMenuUtilityMap(player);

        Island island = IslandTracker.getPlayerIsland(player.getUniqueId());
        if (island != null) {
            IslandTracker.removePlayerIsland(player.getUniqueId());

            if (IslandTracker.isIslandEmpty(island.getUniqueId())) {
                IslandVault.getVaultManager().saveVaultAsync(island.getUniqueId(), Bukkit.getOfflinePlayer(island.getOwner()).getName());
            }
        }
    }
}