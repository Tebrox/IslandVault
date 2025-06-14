package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.MenuManager;
import de.tebrox.islandVault.Menu.VaultMenu;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

public class InventoryCloseListener implements Listener {

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Island island = BentoBox.getInstance().getIslands().getIslandAt(player.getLocation()).orElse(null);

        if(event.getInventory().getHolder() instanceof VaultMenu vaultMenu) {

            IslandVault.getVaultSyncManager().unregisterViewer(island.getUniqueId(), player);
        }

        if(IslandVault.getVaultManager().isVaultLoaded(island.getUniqueId())) {
            IslandVault.getVaultManager().saveVaultAsync(island.getUniqueId(), Bukkit.getOfflinePlayer(island.getOwner()).getName());
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
