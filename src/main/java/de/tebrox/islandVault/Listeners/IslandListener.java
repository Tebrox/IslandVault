package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.IslandTracker;
import de.tebrox.islandVault.Manager.LanguageManager;
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

/**
 * Listener for island-related player events.
 * <p>
 * Handles player entering and exiting islands as well as player join and quit events.
 * Manages loading, saving, and tracking of vaults and player island assignments.
 */
public class IslandListener implements Listener {

    private final Addon addon;

    /**
     * Constructs a new IslandListener and initializes the addon reference.
     */
    public IslandListener() {
        this.addon = BentoBox.getInstance().getAddonsManager().getAddonByName("AOneBlock").orElse(null);
    }

    /**
     * Called when a player enters an island.
     * <p>
     * Loads or creates the vault for the island if it is empty,
     * and assigns the player to the island in the tracker.
     *
     * @param event the island enter event
     */
    @EventHandler
    public void onIslandEnter(IslandEnterEvent event) {
        UUID playerUUID = event.getPlayerUUID();
        Player player = Bukkit.getPlayer(playerUUID);

        // Hole die Insel an der Position des Spielers (z. B. Spawnpunkt)
        Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
        if (island != null) {
            if(IslandTracker.isIslandEmpty(island.getUniqueId())) {
                IslandVault.getVaultManager().loadOrCreateVault(island.getUniqueId(), island.getOwner());
            }
            IslandTracker.setPlayerIsland(player.getUniqueId(), island);
        } else {
            IslandTracker.removePlayerIsland(player.getUniqueId());
        }
    }

    /**
     * Called when a player joins the server.
     * <p>
     * Sets up language placeholders for the player,
     * loads or creates the vault if the player is on an island,
     * and tracks the player's island.
     *
     * @param event the player join event
     */
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

    /**
     * Called when a player quits the server.
     * <p>
     * Removes the player from the island tracker,
     * and saves the vault asynchronously if the island is empty after the player leaves.
     *
     * @param event the player quit event
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        LanguageManager.PlaceholderRegistry placeholderRegistry = IslandVault.getLanguageManager().getPlaceholders(player);
        placeholderRegistry.set("player", player.getName());

        Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
        if (island != null) {
            if(IslandTracker.isIslandEmpty(island.getUniqueId())) {
                IslandVault.getVaultManager().loadOrCreateVault(island.getUniqueId(), island.getOwner());
            }
            IslandTracker.setPlayerIsland(player.getUniqueId(), island);
        } else {
            IslandTracker.removePlayerIsland(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Island island = IslandTracker.getPlayerIsland(player.getUniqueId());
        if (island != null) {
            IslandTracker.removePlayerIsland(player.getUniqueId());

            if (IslandTracker.isIslandEmpty(island.getUniqueId())) {
                IslandVault.getVaultManager().saveVaultAsync(island.getUniqueId(), Bukkit.getOfflinePlayer(island.getOwner()).getName());
            }
        }
    }
}