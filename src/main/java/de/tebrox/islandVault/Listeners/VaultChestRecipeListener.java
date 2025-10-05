package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.Items.VaultChestItem;
import de.tebrox.islandVault.IslandVault;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Arrays;

public class VaultChestRecipeListener implements Listener {

    // ============================================================
// ==========       VaultChest Crafting Listener       =========
// ============================================================

    @EventHandler(ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;
        if (!(recipe instanceof Keyed keyed)) return;

        // Nur unser dynamisches Rezept behandeln
        NamespacedKey key = keyed.getKey();
        if (!key.getKey().equals("vault_chest_dynamic")) return;

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        // Prüfen, ob im Grid eine VaultChest liegt
        boolean hasVaultChest = Arrays.stream(matrix)
                .anyMatch(VaultChestItem::isCustomChest);

        if (hasVaultChest) {
            // Rückbau: VaultChest -> normale Chest
            inv.setResult(new ItemStack(Material.CHEST));
        } else {
            // Herstellung: normale Chest -> VaultChest
            inv.setResult(VaultChestItem.createCustomChest());
        }
    }

}
