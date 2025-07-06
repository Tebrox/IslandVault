package de.tebrox.islandVault.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class IslandPermissionsUpdateAllEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public IslandPermissionsUpdateAllEvent() {}

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}