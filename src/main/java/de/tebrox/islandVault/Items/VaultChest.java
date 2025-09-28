package de.tebrox.islandVault.Items;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import world.bentobox.bentobox.database.objects.Island;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class VaultChest {
    private final UUID owner;
    private final Location location;
    private final Island island;
    private final Inventory inventory;
    private final Queue<ItemStack> pendingItems = new LinkedList<>();
    private ItemStack[] savedFilter = new ItemStack[9]; // 9 Slots Filter GUI
    private UUID lastEditor; // zuletzt editierender Spieler

    public VaultChest(UUID owner, Location location, Island island) {
        this.owner = owner;
        this.location = location;
        this.island = island;
        this.inventory = Bukkit.createInventory(null, 27, "Inseltruhe");
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    public Island getIsland() {
        return island;
    }

    public Inventory getInventory() {
        return inventory;
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

    public ItemStack[] getSavedFilter() {
        return savedFilter.clone();
    }

    public void setSavedFilter(ItemStack[] filter) {
        savedFilter = filter;
    }

    public void saveFilter() {
        ItemStack[] newFilter = new ItemStack[9];
        int i = 0;
        for (ItemStack item : pendingItems) {
            if (i >= 9) break;
            newFilter[i] = item.clone(); // eigene Kopie
            i++;
        }
        savedFilter = newFilter;
        IslandVault.getPlugin().saveChests();
    }

    public ItemStack[] loadFilter() {
        ItemStack[] filter = new ItemStack[9];
        for (int i = 0; i < savedFilter.length; i++) {
            if (savedFilter[i] != null) {
                ItemStack copy = savedFilter[i].clone();
                copy.setAmount(1); // sicherstellen, dass immer nur 1 Item pro Slot angezeigt wird
                filter[i] = copy;
            }
        }
        return filter;
    }

}
