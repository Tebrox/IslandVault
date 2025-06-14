package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.VaultData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ItemSortUtil {

    public enum SortOption {
        ID, AMOUNT, LOCALIZED_NAME
    }

    public enum SortDirection {
        ASCENDING, DESCENDING
    }

    /**
     * Sortiert die angegebene Item-Liste anhand der gegebenen Sortieroption.
     *
     * @param items      Die zu sortierende Liste von ItemStacks
     * @param player     Der Spieler (für Lokalisierung)
     * @param sortOption Die Sortieroption (ID, NAME, AMOUNT, LOCALIZED_NAME)
     * @param sortDirection  Die Sortierrichtung (ASCENDING oder DESCENDING)
     */
    public static void sortItems(List<ItemStack> items, VaultData data, Player player, SortOption sortOption, SortDirection sortDirection) {
        if (items == null || sortOption == null || player == null || sortDirection == null) return;

        if (data == null) return;

        Comparator<ItemStack> comparator = switch (sortOption) {
            case ID -> Comparator.comparing(ItemStack::getType);

            case AMOUNT -> Comparator.comparingInt(item -> {
                ItemStackKey key = ItemStackKey.of(item);
                return data.getItems().getOrDefault(key, 0);
            });

            case LOCALIZED_NAME -> Comparator.comparing(item -> ItemNameTranslator.getLocalizedName(item, player), String.CASE_INSENSITIVE_ORDER);
        };

        if (sortDirection == SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        items.sort(comparator);
    }

    private static int getAmountFromLore(ItemStack item, UUID player) {
        ItemMeta itemMeta = item.getItemMeta();
        List<String> lore = itemMeta.getLore();

        String amountText = lore.get(0).split(":")[1].trim();
        ChatColor.stripColor(amountText);

        return Integer.parseInt(amountText);
    }
}