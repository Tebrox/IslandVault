package de.tebrox.islandVault.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ItemRemovedFromVaultEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Player actor;
    private final UUID vaultOwner;
    private final ItemStack item;
    private final boolean isAutoCollect;
    private boolean cancelled;

    public ItemRemovedFromVaultEvent(Player actor, UUID vaultOwner, ItemStack item, boolean isAutoCollect) {
        this.actor = actor;
        this.vaultOwner = vaultOwner;
        this.item = item;
        this.isAutoCollect = isAutoCollect;
    }

    public Player getActor() { return actor; }
    public UUID getVaultOwner() { return vaultOwner; }
    public ItemStack getItem() { return item; }
    public boolean isAutoCollect() { return isAutoCollect; }

    public static HandlerList getHandlerList() { return handlers; }
    @Override public HandlerList getHandlers() { return handlers; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
}