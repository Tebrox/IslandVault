package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.IslandItemList;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.ItemPermissionRule;
import de.tebrox.islandVault.VaultData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.*;

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

    public static List<ItemStack> getFilteredItemsPaged(
            Player player,
            VaultData vaultData,
            IslandItemList islandItemList,
            @Nullable String searchQuery,
            int offset,
            int limit
    ) {
        boolean onlyWithAmount = PlayerDataUtils.loadShowOnlyItemsWithAmount(player);
        String lowerSearch = searchQuery == null ? null : searchQuery.toLowerCase(Locale.ROOT);

        List<ItemStack> result = new ArrayList<>();

        for (ItemPermissionRule rule : islandItemList.getAllowedRules()) {
            ItemStackKey key = rule.getKey();
            int amount = vaultData.getAmount(key);

            if (onlyWithAmount && amount <= 0) continue;

            ItemStack item = key.toItemStack();
            if (item == null || item.getType().isAir()) continue;

            // Filter mit Suchbegriff
            if (lowerSearch != null) {
                String name = ItemNameTranslator.getLocalizedName(item, player).toLowerCase(Locale.ROOT);
                boolean match = name.contains(lowerSearch);

                if (!match) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        for (String line : meta.getLore()) {
                            if (line != null && line.toLowerCase(Locale.ROOT).contains(lowerSearch)) {
                                match = true;
                                break;
                            }
                        }
                    }
                }

                if (!match) continue;
            }

            result.add(item);
        }

        // Sortierung (optional)
        result.sort(Comparator.comparing(i -> i.getType().name()));

        // Paging
        int end = Math.min(result.size(), offset + limit);
        if (offset >= end) return List.of();
        return result.subList(offset, end);
    }

    public static int countFilteredItems(
            Player player,
            VaultData vaultData,
            IslandItemList islandItemList,
            @Nullable String searchQuery
    ) {
        boolean onlyWithAmount = PlayerDataUtils.loadShowOnlyItemsWithAmount(player);
        String lowerSearch = searchQuery == null ? null : searchQuery.toLowerCase(Locale.ROOT);

        int count = 0;

        for (ItemPermissionRule rule : islandItemList.getAllowedRules()) {
            ItemStackKey key = rule.getKey();
            int amount = vaultData.getAmount(key);

            if (onlyWithAmount && amount <= 0) continue;

            ItemStack item = key.toItemStack();
            if (item == null || item.getType().isAir()) continue;

            if (lowerSearch != null) {
                String name = ItemNameTranslator.getLocalizedName(item, player).toLowerCase(Locale.ROOT);
                boolean match = name.contains(lowerSearch);

                if (!match) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        for (String line : meta.getLore()) {
                            if (line != null && line.toLowerCase(Locale.ROOT).contains(lowerSearch)) {
                                match = true;
                                break;
                            }
                        }
                    }
                }

                if (!match) continue;
            }

            count++;
        }

        return count;
    }
}