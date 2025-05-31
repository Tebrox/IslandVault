package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.Enums.LoggerAction;
import de.tebrox.islandVault.Events.ItemAddedToVaultEvent;
import de.tebrox.islandVault.Events.ItemRemovedFromVaultEvent;
import de.tebrox.islandVault.Utils.AdminVaultLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class VaultEventListener implements Listener {
    private final AdminVaultLogger logger;

    public VaultEventListener(AdminVaultLogger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void onItemAdded(ItemAddedToVaultEvent event) {
        logger.logAction(LoggerAction.ADD_ITEM, event.getActor(), event.getVaultOwner(), event.getItem());
    }

    @EventHandler
    public void onItemRemoved(ItemRemovedFromVaultEvent event) {
        logger.logAction(LoggerAction.REMOVE_ITEM, event.getActor(), event.getVaultOwner(), event.getItem());
    }
}