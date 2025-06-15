package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Menu.VaultMenu;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
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
            MenuManager.getPlayerMenuUtility(player).setData("searchQuery", null);
            MenuManager.getPlayerMenuUtility(player).setData("adminOpen", null);
            MenuManager.getPlayerMenuUtility(player).setData("previousMenu", null);
            MenuManager.getPlayerMenuUtility(player).setData("ownerUUID", null);
        } catch (MenuManagerNotSetupException e) {
            throw new RuntimeException(e);
        }
    }
}
