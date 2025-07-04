package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerDataUtils {

    public static void saveSoundEnabled(Player player, boolean enabled) {
        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(getSoundEnabledKey(), PersistentDataType.BOOLEAN, enabled);
    }

    public static boolean loadSoundEnabled(Player player) {
        PersistentDataContainer container = player.getPersistentDataContainer();
        return container.getOrDefault(getSoundEnabledKey(), PersistentDataType.BOOLEAN, true);
    }

    public static void saveShowOnlyItemsWithAmount(Player player, boolean showWithAmount) {
        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(getShowItemsWithAmountKey(), PersistentDataType.BOOLEAN, showWithAmount);
    }

    public static boolean loadShowOnlyItemsWithAmount(Player player) {
        PersistentDataContainer container = player.getPersistentDataContainer();
        return container.getOrDefault(getShowItemsWithAmountKey(), PersistentDataType.BOOLEAN, false);
    }

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

    public static NamespacedKey getSoundEnabledKey() { return new NamespacedKey(IslandVault.getPlugin(), "soundEnabled"); }
    public static NamespacedKey getSortOptionKey() {
        return new NamespacedKey(IslandVault.getPlugin(), "sort_option");
    }
    public static NamespacedKey getSortDirectionKey() { return new NamespacedKey(IslandVault.getPlugin(), "sort_direction"); }

    public static NamespacedKey getShowItemsWithAmountKey() { return new NamespacedKey(IslandVault.getPlugin(), "showItemsWithAmount"); }
}