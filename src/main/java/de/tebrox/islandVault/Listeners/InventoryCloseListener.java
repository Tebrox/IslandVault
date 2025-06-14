package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.MenuManager;
import de.tebrox.islandVault.Menu.VaultMenu;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryCloseListener implements Listener {

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        if(event.getInventory().getHolder() instanceof VaultMenu vaultMenu) {
            IslandVault.getVaultManager().unregisterViewer(player, vaultMenu.getIslandOwner());
        }

        if(IslandVault.getVaultManager().getVaults().containsKey(player.getUniqueId())){
            PlayerVaultUtils playerVaultUtils = IslandVault.getVaultManager().getVaults().get(player.getUniqueId());
            IslandVault.getVaultManager().saveVault(playerVaultUtils);
        }
        try {
            PlayerMenuUtility playerMenuUtility = MenuManager.getPlayerMenuUtility(player);
            playerMenuUtility.setData("searchQuery", null);
            playerMenuUtility.setData("adminOpen", null);
            playerMenuUtility.setData("previousMenu", null);
            playerMenuUtility.setData("ownerUUID", null);
            MenuManager.getPlayerMenuUtilityMap().put(player.getUniqueId(), playerMenuUtility);
        } catch (MenuManagerNotSetupException e) {
            throw new RuntimeException(e);
        }
    }
}
