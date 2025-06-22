package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.Enums.LoggerAction;
import de.tebrox.islandVault.Events.VaultUpdateEvent;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Utils.AdminVaultLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener that handles vault update events and logs administrative actions.
 * <p>
 * This listener ensures vault viewers are refreshed after updates and logs item additions/removals
 * to the admin vault log when a player is responsible for the change.
 */
public class VaultEventListener implements Listener {
    private final AdminVaultLogger logger;

    /**
     * Constructs the listener with a reference to the vault logger.
     *
     * @param logger the logger responsible for recording vault actions
     */
    public VaultEventListener(AdminVaultLogger logger) {
        this.logger = logger;
    }

    /**
     * Called when a vault is updated, either by item addition or removal.
     * <ul>
     *     <li>Refreshes the viewer GUI for the affected island.</li>
     *     <li>If an actor is present (i.e. a player caused the change), logs the action.</li>
     * </ul>
     *
     * @param event the vault update event
     */
    @EventHandler
    public void onVaultUpdate(VaultUpdateEvent event) {
        IslandVault.getVaultSyncManager().refreshViewers(event.getIsland().getUniqueId());
        if(event.getActor() == null) {
            return;
        }
        LoggerAction loggerAction;
        if(event.getAmountChange() > 0) {
            loggerAction = LoggerAction.ADD_ITEM;
        }else{
            loggerAction = LoggerAction.REMOVE_ITEM;
        }
        logger.logAction(loggerAction, event.getActor(), event.getIsland().getOwner(), event.getItemStackKey(), Math.abs(event.getAmountChange()));
    }
}