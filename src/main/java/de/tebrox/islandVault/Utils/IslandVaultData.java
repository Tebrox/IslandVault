package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Events.VaultUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Repräsentiert ein gemeinsames Vault für eine Insel.
 * Alle Mitglieder der Insel teilen sich diese Inhalte.
 * Rechte zur Anzeige/Benutzung werden pro Spieler geprüft.
 */
public class IslandVaultData {

    private final String islandId;
    private final Map<String, Integer> inventory = new HashMap<>(); // PermissionKey -> Anzahl
    private final Inventory backpack = Bukkit.createInventory(null, 54, "§5Insel-Rucksack");

    private boolean autoCollect = false;
    private boolean showAutocollectMessage = false;

    public IslandVaultData(String islandId) {
        this.islandId = islandId;
    }

    public String getIslandId() { return islandId; }
    public Map<String, Integer> getInventory() { return inventory; }

    public boolean isAutoCollectEnabled() { return autoCollect; }
    public void setAutoCollect(boolean value) { this.autoCollect = value; }

    public boolean isShowAutocollectMessage() { return showAutocollectMessage; }
    public void setShowAutocollectMessage(boolean value) { this.showAutocollectMessage = value; }

    public Inventory getBackpack() { return backpack; }

    // ============================================================
    // ================== INVENTARLOGIK ============================
    // ============================================================

    public void addItem(String permissionKey, int amount) {
        inventory.put(permissionKey, inventory.getOrDefault(permissionKey, 0) + amount);
    }

    public ItemStack takeItem(String permissionKey, int amount, Player actor) {
        int current = inventory.getOrDefault(permissionKey, 0);
        if (current <= 0) return null;

        int toExtract = Math.min(current, amount);

        // Event auslösen (z. B. für Booster/Logging)
        // TODO anpassen
        //VaultUpdateEvent event = new VaultUpdateEvent(actor, islandId, permissionKey, -toExtract, false);
        //Bukkit.getPluginManager().callEvent(event);
        //if (event.isCancelled()) return null;

        inventory.put(permissionKey, Math.max(0, current - toExtract));

        ItemStack base = IslandVault.getItemManager().getItemList().stream()
                .filter(i -> IslandVault.getItemManager().buildPermissionKey(i).equalsIgnoreCase(permissionKey))
                .findFirst().orElse(null);

        if (base == null) return null;

        ItemStack stack = base.clone();
        stack.setAmount(toExtract);
        return stack;
    }
}
