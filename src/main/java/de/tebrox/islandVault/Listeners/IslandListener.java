package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.IslandPresenceTracker;
import de.tebrox.islandVault.Manager.LanguageManager;
import de.tebrox.islandVault.Manager.MenuManager;
import de.tebrox.islandVault.Utils.IslandUtils;
import de.tebrox.islandVault.Utils.PlayerVaultUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class IslandListener implements Listener {

    private final Addon addon;

    public IslandListener() {
        this.addon = BentoBox.getInstance().getAddonsManager().getAddonByName("AOneBlock").orElse(null);
    }

    @EventHandler
    public void onIslandEnter(IslandEnterEvent event) {
        UUID playerUUID = event.getPlayerUUID();
        String islandUUID = event.getIsland().getUniqueId();

        if(IslandPresenceTracker.isIslandIsEmpty(islandUUID)) {
            if(!IslandVault.getVaultManager().getVaults().containsKey(event.getIsland().getOwner())) {
                IslandVault.getVaultManager().loadVault(event.getIsland().getOwner());
            }
        }

        LanguageManager.PlaceholderRegistry placeholderRegistry = IslandVault.getLanguageManager().getPlaceholders(Bukkit.getPlayer(playerUUID));
        placeholderRegistry.set("islandOwner", Bukkit.getOfflinePlayer(event.getIsland().getOwner()).getName());

        IslandPresenceTracker.playerEnteredIsland(islandUUID, playerUUID);
    }

    @EventHandler
    public void onIslandExit(IslandExitEvent event) {
        UUID playerUUID = event.getPlayerUUID();
        String islandUUID = event.getIsland().getUniqueId();
        IslandPresenceTracker.playerLeftIsland(islandUUID, playerUUID);

        if(IslandPresenceTracker.isIslandIsEmpty(islandUUID)) {
            if(IslandVault.getVaultManager().getVaults().containsKey(event.getIsland().getOwner())){
                PlayerVaultUtils playerVaultUtils = IslandVault.getVaultManager().getVaults().get(event.getIsland().getOwner());
                IslandVault.getVaultManager().saveVault(playerVaultUtils);
                IslandVault.getVaultManager().getVaults().remove(event.getIsland().getOwner());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        MenuManager.addPlayerMenuUtilityMap(player);
        IslandVault.getLanguageManager().setLocale(player, Locale.forLanguageTag(player.getLocale().replace('_', '-')));
        LanguageManager.PlaceholderRegistry placeholderRegistry = IslandVault.getLanguageManager().getPlaceholders(player);
        placeholderRegistry.set("player", player.getName());

        Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
        if (island != null) {
            if(IslandPresenceTracker.isIslandIsEmpty(island.getUniqueId())) {
                if(!IslandVault.getVaultManager().getVaults().containsKey(island.getOwner())) {
                    IslandVault.getVaultManager().loadVault(island.getOwner());
                }
            }

            IslandPresenceTracker.playerEnteredIsland(island.getUniqueId(), player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        MenuManager.removeFromPlayerMenuUtilityMap(player);
        IslandVault.getLanguageManager().removeLocale(player);

        Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
        if (island != null) {
            IslandPresenceTracker.playerLeftIsland(island.getUniqueId(), player.getUniqueId());
        }

        if(IslandPresenceTracker.isIslandIsEmpty(island.getUniqueId())) {
            if(IslandVault.getVaultManager().getVaults().containsKey(island.getOwner())){
                PlayerVaultUtils playerVaultUtils = IslandVault.getVaultManager().getVaults().get(island.getOwner());
                IslandVault.getVaultManager().saveVault(playerVaultUtils);
                IslandVault.getVaultManager().getVaults().remove(island.getOwner());
            }
        }
    }
}