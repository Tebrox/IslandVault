package de.tebrox.islandVault.Items;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Stellt das spezielle VaultChest-Item dar,
 * das Spieler platzieren können, um eine Inseltruhe zu erzeugen.
 */
public class VaultChestItem {

    public static final int CUSTOM_MODEL_DATA = 770001;
    public static final NamespacedKey CHEST_KEY =
            new NamespacedKey(IslandVault.getPlugin(), "vault_chest");

    /**
     * Erstellt das eigentliche Inseltruhen-Item mit Lore, Name und NBT-Markierung.
     */
    public static ItemStack createCustomChest() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6Inseltruhe");
        meta.setCustomModelData(CUSTOM_MODEL_DATA);

        List<String> lore = new ArrayList<>();
        lore.add("§7Spezielle Truhe für dein Insel-Lager");
        lore.add("");
        lore.add("§eSneak + Rechtsklick§7 → öffnet Filter-Einstellungen");
        lore.add("§7Der Filter bestimmt, welche Items");
        lore.add("§7automatisch ein- oder ausgelagert werden.");
        lore.add("");
        lore.add("§bModus §aEinlagerung§7: Items gehen ins Vault");
        lore.add("§bModus §dAuslagerung§7: Items kommen aus dem Vault");
        lore.add("");
        lore.add("§8(Platzierung: Nur Single-Truhen!)");

        meta.setLore(lore);

        // Tag setzen
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(CHEST_KEY, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Prüft, ob ein Item eine gültige VaultChest ist.
     */
    public static boolean isCustomChest(ItemStack item) {
        if (item == null || item.getType() != Material.CHEST) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // sichere Erkennung über PersistentDataKey ODER CustomModelData
        boolean hasKey = pdc.has(CHEST_KEY, PersistentDataType.BYTE);
        boolean hasModelData = meta.hasCustomModelData()
                && meta.getCustomModelData() == CUSTOM_MODEL_DATA;

        return hasKey || hasModelData;
    }

    /**
     * Registriert ein Shapeless-Rezept für die VaultChest.
     * @param plugin dein Plugin
     */
    public static void registerRecipes(Plugin plugin) {
        registerVaultChestRecipe(plugin);
    }

// ============================================================
// ==========      VaultChest Rezept-Registrierung     =========
// ============================================================

    private static void registerVaultChestRecipe(Plugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "vault_chest_dynamic");

        // Nur hinzufügen, wenn noch nicht vorhanden
        if (Bukkit.getRecipe(key) != null) return;

        // Ergebnis ist egal – wird im Listener dynamisch ersetzt
        ItemStack result = new ItemStack(Material.CHEST);

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(Material.CHEST);

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("[VaultChestItem] Dynamisches Rezept 'Inseltruhe <-> normale Chest' registriert.");
    }

}
