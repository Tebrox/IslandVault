package de.tebrox.islandVault.Events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

public class VaultUpdateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Player actor;
    private final Island island;
    private final Material material;
    private final int amountChange;
    private final boolean isAutoCollect;
    private boolean cancelled;

    public VaultUpdateEvent(Player actor, Island island, Material material, int amountChange, boolean isAutoCollect) {
        this.actor = actor;
        this.island = island;
        this.material = material;
        this.amountChange = amountChange;
        this.isAutoCollect = isAutoCollect;
    }

    public Player getActor() {
        return actor;
    }

    public Island getIsland() {
        return island;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmountChange() {
        return amountChange;
    }
    public boolean isAutoCollect() {
        return isAutoCollect;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}