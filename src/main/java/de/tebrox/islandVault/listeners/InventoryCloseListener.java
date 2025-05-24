package de.tebrox.islandVault.listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.utils.PlayerVaultUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryCloseListener implements Listener {

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        if(IslandVault.getVaultManager().getVaults().containsKey(player.getUniqueId())){
            PlayerVaultUtils playerVaultUtils = IslandVault.getVaultManager().getVaults().get(player.getUniqueId());
            IslandVault.getVaultManager().saveVault(playerVaultUtils);
        }
    }
}
