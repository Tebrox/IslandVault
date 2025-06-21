package de.tebrox.islandVault.Events;

import de.tebrox.islandVault.Utils.ItemStackKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Event triggered when the vault of an island is updated.
 * This event can be cancelled to prevent the update from occurring.
 */
public class VaultUpdateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Player actor;
    private final Island island;
    private final ItemStackKey itemStackKey;
    private final int amountChange;
    private final boolean isAutoCollect;
    private boolean cancelled;

    /**
     * Constructs a new VaultUpdateEvent.
     *
     * @param actor the player who caused the vault update
     * @param island the island whose vault is being updated
     * @param itemStackKey the material involved in the update
     * @param amountChange the amount changed in the vault (positive or negative)
     * @param isAutoCollect true if the update was caused by auto-collection, false otherwise
     */
    public VaultUpdateEvent(Player actor, Island island, ItemStackKey itemStackKey, int amountChange, boolean isAutoCollect) {
        this.actor = actor;
        this.island = island;
        this.itemStackKey = itemStackKey;
        this.amountChange = amountChange;
        this.isAutoCollect = isAutoCollect;
    }

    /**
     * Gets the player who caused the vault update.
     *
     * @return the actor player
     */
    public Player getActor() {
        return actor;
    }

    /**
     * Gets the island whose vault is being updated.
     *
     * @return the island
     */
    public Island getIsland() {
        return island;
    }

    public ItemStackKey getItemStackKey() {
        return itemStackKey;
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