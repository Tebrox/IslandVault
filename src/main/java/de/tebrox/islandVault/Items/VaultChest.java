package de.tebrox.islandVault.Items;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;

/**
 * Repräsentiert eine platzierte VaultChest.
 * Verwaltet Eigentümer, Position, Insel, Filter und PendingItems.
 * Vollständig integriert mit dem VaultChestManager.
 */
public class VaultChest {

    private final UUID owner;
    private final Location location;
    private final Island island;

    private final Queue<ItemStack> pendingItems = new LinkedList<>();
    private ItemStack[] savedFilter = new ItemStack[9];
    private UUID lastEditor;

    private final List<Integer> filterIndexes = Arrays.asList(0,1,2,3,4,5,6,7,8);
    private boolean isInputChest = true; // default: Einlagerung

    public VaultChest(UUID owner, Location location, Island island) {
        this.owner = owner;
        this.location = location;
        this.island = island;
    }

    // =========================================================
    // ===================== Getter / Setter ===================
    // =========================================================

    public UUID getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    public Island getIsland() {
        return island;
    }

    public Queue<ItemStack> getPendingItems() {
        return pendingItems;
    }

    public UUID getLastEditor() {
        return lastEditor;
    }

    public void setLastEditor(UUID lastEditor) {
        this.lastEditor = lastEditor;
    }

    public List<Integer> getFilterIndexes() {
        return filterIndexes;
    }

    public boolean isInputChest() {
        return isInputChest;
    }

    public void setMode(boolean input) {
        this.isInputChest = input;
        scheduleSave();
    }

    public void toggleInputOutput() {
        this.isInputChest = !this.isInputChest;
        scheduleSave();
    }

    // =========================================================
    // ===================== Inventar & Filter =================
    // =========================================================

    /**
     * Gibt das echte Inventar des Chest-Blocks zurück oder null,
     * wenn der Block keine Chest (mehr) ist oder nicht geladen.
     */
    public Inventory getChestInventory() {
        Block block = location.getBlock();
        if (block == null) return null;

        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            return chest.getInventory();
        }
        return null;
    }

    /**
     * Gibt eine Kopie des gespeicherten Filters zurück.
     */
    public ItemStack[] getSavedFilter() {
        return savedFilter.clone();
    }

    /**
     * Setzt den gespeicherten Filter und markiert diese Chest zum Speichern.
     */
    public void setSavedFilter(ItemStack[] filter) {
        this.savedFilter = (filter == null) ? new ItemStack[9] : filter.clone();
        scheduleSave();
    }

    /**
     * Speichert den aktuellen Pending-Filter als gespeicherten Filter.
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
        scheduleSave();
    }

    /**
     * Gibt eine visuelle Kopie des Filters zurück (jeweils Amount = 1).
     */
    public ItemStack[] loadFilter() {
        ItemStack[] copy = new ItemStack[9];
        for (int i = 0; i < savedFilter.length; i++) {
            if (savedFilter[i] != null) {
                ItemStack clone = savedFilter[i].clone();
                clone.setAmount(1);
                copy[i] = clone;
            }
        }
        return copy;
    }

    // =========================================================
    // ===================== Speicherung =======================
    // =========================================================

    /**
     * Übergibt diese Truhe an den VaultChestManager zur asynchronen Speicherung.
     */
    public void scheduleSave() {
        if (IslandVault.getVaultChestManager() != null) {
            IslandVault.getVaultChestManager().saveChest(this);
        } else {
            Bukkit.getLogger().warning("[VaultChest] VaultChestManager war null beim Speichern von " + location);
        }
    }

    @Override
    public String toString() {
        return "VaultChest{" +
                "owner=" + owner +
                ", loc=" + (location != null ? locationToString() : "null") +
                ", input=" + isInputChest +
                ", pending=" + pendingItems.size() +
                '}';
    }

    private String locationToString() {
        return location.getWorld().getName() + "@" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
