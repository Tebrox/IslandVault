package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerDataUtils {

    public static void saveSortSettings(Player player, ItemSortUtil.SortOption option, ItemSortUtil.SortDirection direction) {
        PersistentDataContainer container = player.getPersistentDataContainer();

        container.set(getSortOptionKey(), PersistentDataType.STRING, option.name());
        container.set(getSortDirectionKey(), PersistentDataType.STRING, direction.name());
    }

    public static ItemSortUtil.SortOption loadSortOption(Player player) {
        PersistentDataContainer container = player.getPersistentDataContainer();

        String value = container.get(getSortOptionKey(), PersistentDataType.STRING);
        if (value == null) return ItemSortUtil.SortOption.ID; // Default

        try {
            return ItemSortUtil.SortOption.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ItemSortUtil.SortOption.ID;
        }
    }

    public static ItemSortUtil.SortDirection loadSortDirection(Player player) {
        PersistentDataContainer container = player.getPersistentDataContainer();

        String value = container.get(getSortDirectionKey(), PersistentDataType.STRING);
        if (value == null) return ItemSortUtil.SortDirection.ASCENDING; // Default

        try {
            return ItemSortUtil.SortDirection.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ItemSortUtil.SortDirection.ASCENDING;
        }
    }

    public static NamespacedKey getSortOptionKey() {
        return new NamespacedKey(IslandVault.getPlugin(), "sort_option");
    }

    public static NamespacedKey getSortDirectionKey() {
        return new NamespacedKey(IslandVault.getPlugin(), "sort_direction");
    }
}