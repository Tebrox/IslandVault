package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.Events.IslandPermissionsUpdateAllEvent;
import de.tebrox.islandVault.Events.IslandPermissionsUpdateEvent;
import de.tebrox.islandVault.IslandItemList;
import de.tebrox.islandVault.IslandSession;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Utils.PluginLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.UUID;

public class IslandPermissionsListener implements Listener {

    @EventHandler
    public void onIslandPermissionsUpdate(IslandPermissionsUpdateEvent event) {
        UUID ownerUUID = event.getOwnerUUID();
        String islandID = event.getIslandID();

        IslandItemList islandItemList = IslandVault.getSessionManager().getItemList(islandID).orElse(null);

        if(islandItemList == null) return;

        islandItemList.updatePermissionsAsync(ownerUUID).thenRun(() -> {
            PluginLogger.info("Permissions für Insel von " + ownerUUID + " aktualisiert.");
            // Hier GUI oder Cache aktualisieren, falls nötig
        });
    }

    @EventHandler
    public void onIslandPermissionsUpdateAll(IslandPermissionsUpdateAllEvent event) {
        Collection<IslandSession> allSessions = IslandVault.getSessionManager().getAllSessions();

        for(IslandSession session : allSessions) {
            IslandItemList islandItemList = session.getItemList();
            islandItemList.updatePermissionsAsync(session.getIsland().getOwner()).thenRun(() -> {
                PluginLogger.info("Permissions für Insel von " + session.getIsland().getOwner() + " aktualisiert.");
            });
        }
    }
}