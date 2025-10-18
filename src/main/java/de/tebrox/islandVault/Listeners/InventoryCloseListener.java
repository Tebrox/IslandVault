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

/**
 * Lauscht auf das Schließen von Vault-Menüs und kümmert sich um Aufräumarbeiten.
 * Angepasst auf das Insel-Vault-System (BentoBox, String-Insel-IDs).
 */
public class InventoryCloseListener implements Listener {

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // 1️⃣ Prüfen, ob das geschlossene Inventar ein VaultMenu ist
        if (event.getInventory().getHolder() instanceof VaultMenu vaultMenu) {
            String islandId = vaultMenu.getIslandId();
            if (islandId != null) {
                IslandVault.getVaultManager().unregisterViewer(player, islandId);
            }
        }

        // 2️⃣ Vault-Daten speichern (falls Spieler Insel-Besitzer oder Teammitglied ist)
        try {
            // Finde Insel unter dem Spieler
            var islandOpt = de.tebrox.islandVault.Utils.IslandUtils
                    .getIslandManager()
                    .getIslandAt(player.getLocation());

            if (islandOpt.isPresent()) {
                String islandId = islandOpt.get().getUniqueId();
                var vault = IslandVault.getVaultManager().getIslandVaults().get(islandId);
                if (vault != null) {
                    IslandVault.getVaultManager().saveIslandVault(vault);
                }
            }
        } catch (Exception ex) {
            IslandVault.getPlugin().getLogger().warning(
                    "[InventoryCloseListener] Fehler beim Speichern eines Vaults: " + ex.getMessage());
        }

        // 3️⃣ PlayerMenuUtility Daten aufräumen
        try {
            PlayerMenuUtility util = MenuManager.getPlayerMenuUtility(player);
            util.setData("searchQuery", null);
            util.setData("adminOpen", null);
            MenuManager.getPlayerMenuUtilityMap().put(player.getUniqueId(), util);
        } catch (MenuManagerNotSetupException e) {
            IslandVault.getPlugin().getLogger().warning(
                    "[InventoryCloseListener] PlayerMenuUtility nicht initialisiert: " + e.getMessage());
        }
    }
}
