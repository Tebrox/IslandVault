package de.tebrox.islandVault.Items;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;

public class VaultChest {
    private final UUID owner;
    private final Location location;
    private final Island island;
    private final Queue<ItemStack> pendingItems = new LinkedList<>();
    private ItemStack[] savedFilter = new ItemStack[9]; // 9 Slots Filter GUI
    private UUID lastEditor;
    private final List<Integer> filterIndexes = Arrays.asList(0,1,2,3,4,5,6,7,8);

    private boolean isInputChest = true; // default Input

    public VaultChest(UUID owner, Location location, Island island) {
        this.owner = owner;
        this.location = location;
        this.island = island;
    }

    public UUID getOwner() { return owner; }
    public Location getLocation() { return location; }
    public Island getIsland() { return island; }
    public Queue<ItemStack> getPendingItems() { return pendingItems; }
    public UUID getLastEditor() { return lastEditor; }
    public void setLastEditor(UUID lastEditor) { this.lastEditor = lastEditor; }

    /**
     * Gibt die aktuelle Chest-Inventory zurück, oder null, wenn der Block keine Chest ist.
     */
    public Inventory getChestInventory() {
        Block block = location.getBlock();
        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            return chest.getInventory();
        }
        return null;
    }

    // -----------------------------
    // Filter-Methoden
    // -----------------------------
    public ItemStack[] getSavedFilter() {
        return savedFilter.clone();
    }

    public void setSavedFilter(ItemStack[] filter) {
        if (filter == null) {
            this.savedFilter = new ItemStack[9];
        } else {
            this.savedFilter = filter.clone();
        }
    }

    /**
     * Speichert den aktuellen Pending-Filter in savedFilter.
     */
    public void saveFilter() {
        ItemStack[] newFilter = new ItemStack[9];
        int i = 0;
        for (ItemStack item : pendingItems) {
            if (i >= 9) break;
            if (item != null) newFilter[i] = item.clone();
            i++;
        }
        this.savedFilter = newFilter;
    }

    /**
     * Gibt eine anzeigefähige Kopie des Filters zurück.
     */
    public ItemStack[] loadFilter() {
        ItemStack[] filter = new ItemStack[9];
        for (int i = 0; i < savedFilter.length; i++) {
            if (savedFilter[i] != null) {
                ItemStack copy = savedFilter[i].clone();
                copy.setAmount(1);
                filter[i] = copy;
            }
        }
        return filter;
    }

    public List<Integer> getFilterIndexes() {
        return filterIndexes;
    }

    public boolean isInputChest() { return isInputChest; }
    public void toggleInputOutput() { isInputChest = !isInputChest; }
    public void setMode(boolean mode) {
        isInputChest = mode;
    }
}
