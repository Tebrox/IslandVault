package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Menu.VaultMenu;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Listener to handle actions when an inventory GUI is closed by a player.
 * <p>
 * This listener performs cleanup tasks such as unregistering the player as a viewer of the vault,
 * saving the vault asynchronously if loaded, and clearing menu-related cached data.
 */
public class InventoryListener implements Listener {

    /**
     * Handles the InventoryCloseEvent when a player closes a GUI.
     * <p>
     * If the inventory belongs to a VaultMenu, the player is unregistered as a viewer of the vault.
     * If the vault is loaded, it is saved asynchronously.
     * Finally, it clears stored menu data related to the player.
     *
     * @param event the inventory close event
     * @throws RuntimeException if the MenuManager is not properly set up
     */
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
