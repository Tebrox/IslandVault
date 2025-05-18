package de.tebrox.islandVault.listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.utils.PlayerVaultUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if(!IslandVault.getVaultManager().getVaults().containsKey(player.getUniqueId())) {
            IslandVault.getVaultManager().loadVault(player);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if(IslandVault.getVaultManager().getVaults().containsKey(player.getUniqueId())){
            PlayerVaultUtils playerVaultUtils = IslandVault.getVaultManager().getVaults().get(player.getUniqueId());
            IslandVault.getVaultManager().saveVault(playerVaultUtils);
            IslandVault.getVaultManager().getVaults().remove(player.getUniqueId());
        }

    }

}
