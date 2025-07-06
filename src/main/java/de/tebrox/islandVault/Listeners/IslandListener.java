package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.IslandSessionManager;
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

public class IslandListener implements Listener {

    private final Addon addon;
    private final IslandSessionManager sessionManager;

    public IslandListener(IslandSessionManager sessionManager) {
        this.addon = BentoBox.getInstance().getAddonsManager().getAddonByName("AOneBlock").orElse(null);
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onIslandEnter(IslandEnterEvent event) {
        UUID playerUUID = event.getPlayerUUID();
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
        if (island != null) {
            sessionManager.playerEnteredIsland(playerUUID, island);
        } else {
            sessionManager.playerLeftIsland(playerUUID); // Fallback, falls Position keine Insel ist
        }
    }

    @EventHandler
    public void onIslandExit(IslandExitEvent event) {
        UUID playerUUID = event.getPlayerUUID();
        sessionManager.playerLeftIsland(playerUUID);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        LanguageManager.PlaceholderRegistry placeholderRegistry = IslandVault.getLanguageManager().getPlaceholders(player);
        placeholderRegistry.set("player", player.getName());

        Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
        if (island != null) {
            sessionManager.playerEnteredIsland(player.getUniqueId(), island);
        } else {
            sessionManager.playerLeftIsland(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        sessionManager.playerLeftIsland(player.getUniqueId());
    }
}
