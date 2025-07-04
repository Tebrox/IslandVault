package de.tebrox.islandVault.Events;

import de.tebrox.islandVault.Utils.ItemStackKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import world.bentobox.bentobox.database.objects.Island;

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
    private final int newAmount;
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
    public VaultUpdateEvent(Player actor, Island island, ItemStackKey itemStackKey, int amountChange, int newAmount, boolean isAutoCollect) {
        this.actor = actor;
        this.island = island;
        this.itemStackKey = itemStackKey;
        this.amountChange = amountChange;
        this.newAmount = newAmount;
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

    /**
     * Gets the item stack key (material) involved in the vault update.
     *
     * @return the item stack key
     */
    public ItemStackKey getItemStackKey() {
        return itemStackKey;
    }

    /**
     * Gets the amount by which the vault contents changed.
     * Positive values indicate an addition, negative values a removal.
     *
     * @return the amount changed
     */
    public int getAmountChange() {
        return amountChange;
    }

    public int getNewAmount() { return newAmount; }

    /**
     * Indicates whether the vault update was triggered by an automatic collection process.
     *
     * @return true if auto-collection caused the update, false otherwise
     */
    public boolean isAutoCollect() {
        return isAutoCollect;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return the handler list
     */
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Gets the static handler list for this event type.
     *
     * @return the handler list
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Checks whether this event has been cancelled.
     *
     * @return true if the event is cancelled, false otherwise
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancelled state of this event.
     *
     * @param cancelled true to cancel the event, false to allow it
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}