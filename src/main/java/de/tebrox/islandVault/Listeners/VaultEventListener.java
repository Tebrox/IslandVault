package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.Enums.LoggerAction;
import de.tebrox.islandVault.Events.VaultUpdateEvent;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Utils.AdminVaultLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultEventListener implements Listener {
    private final AdminVaultLogger logger;

    public VaultEventListener(AdminVaultLogger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void onVaultUpdate(VaultUpdateEvent event) {
        IslandVault.getVaultManager().updateViewers(event.getVaultOwner());
        if(event.getActor() == null) {
            return;
        }
        LoggerAction loggerAction;
        if(event.getAmountChange() > 0) {
            loggerAction = LoggerAction.ADD_ITEM;
        }else{
            loggerAction = LoggerAction.REMOVE_ITEM;
        }
        logger.logAction(loggerAction, event.getActor(), event.getVaultOwner(), event.getMaterial(), Math.abs(event.getAmountChange()));
    }
}