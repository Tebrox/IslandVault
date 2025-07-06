package de.tebrox.islandVault.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class IslandPermissionsUpdateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final UUID ownerUUID;
    private final String islandID;

    public IslandPermissionsUpdateEvent(UUID ownerUUID, String islandID) {
        this.ownerUUID = ownerUUID;
        this.islandID = islandID;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getIslandID() {
        return islandID;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}