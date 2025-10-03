package de.tebrox.islandVault.Items;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class VaultChestItem {

    public static final NamespacedKey CHEST_KEY = new NamespacedKey(IslandVault.getPlugin(), "vault_chest");

    public static final int CUSTOM_MODEL_DATA = 770001; // eindeutige Zahl

    public static ItemStack createCustomChest() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Inseltruhe");
        meta.setCustomModelData(CUSTOM_MODEL_DATA);

        List<String> lore = new ArrayList<>();
        lore.add("§7Spezielle Truhe für dein Insel-Lager");
        lore.add(" ");
        lore.add("§eSneak + Rechtsklick§7 zum Öffnen der Filter-Einstellungen");
        lore.add("§eFilter§7 bestimmt, welche Items importiert oder exportiert werden");
        lore.add(" ");
        lore.add("§bModus: §6Einlagerung §7→ zieht Items §7aus der Chest ins Vault");
        lore.add("§bModus: §dAuslagerung §7→ füllt Items §7aus dem Vault in die Chest");
        lore.add(" ");
        lore.add("§8Der Modus kann jederzeit im Filter-Menü geändert werden.");

        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isCustomChest(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasCustomModelData() && meta.getCustomModelData() == CUSTOM_MODEL_DATA;
    }

    public static void registerRecipe(Plugin plugin) {
        // Das Item, das beim Craften rauskommt
        ItemStack result = createCustomChest();

        // Eindeutiger Key für das Rezept
        NamespacedKey key = new NamespacedKey(plugin, "inseltruhe");

        // Shapeless Rezept erstellen
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        // Zutat hinzufügen (nur eine Chest)
        recipe.addIngredient(Material.CHEST);

        // Rezept registrieren
        Bukkit.addRecipe(recipe);
    }
}
