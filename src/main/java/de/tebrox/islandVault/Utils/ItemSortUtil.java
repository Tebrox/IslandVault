package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;

/**
 * Dienstklasse zum Sortieren von Items in der Vault-GUI.
 * Unterstützt verschiedene Kriterien (ID, Menge, lokalisierter Name).
 * Nutzt jetzt das Insel-Vault-System mit PermissionKeys.
 */
public class ItemSortUtil {

    public enum SortOption {
        ID,
        AMOUNT,
        LOCALIZED_NAME
    }

    public enum SortDirection {
        ASCENDING,
        DESCENDING
    }

    /**
     * Sortiert Items nach dem angegebenen Kriterium für eine bestimmte Insel.
     *
     * @param items         Die Item-Liste
     * @param islandId      Die Insel-ID (String)
     * @param player        Der aktuelle Spieler (für Lokalisierung)
     * @param sortOption    Sortierkriterium (ID, NAME, AMOUNT)
     * @param sortDirection Sortierrichtung (ASC/DESC)
     */
    public static void sortItems(List<ItemStack> items,
                                 String islandId,
                                 Player player,
                                 SortOption sortOption,
                                 SortDirection sortDirection) {
        if (items == null || sortOption == null || player == null || sortDirection == null || islandId == null) {
            return;
        }

        VaultManager vaultManager = IslandVault.getVaultManager();

        Comparator<ItemStack> comparator = switch (sortOption) {
            case ID -> Comparator.comparing(ItemStack::getType);

            case AMOUNT -> Comparator.comparingInt(item -> {
                String key = IslandVault.getItemManager().buildPermissionKey(item);
                var vault = vaultManager.getIslandVaults().get(islandId);
                if (vault == null) return 0;
                return vault.getInventory().getOrDefault(key, 0);
            });

            case LOCALIZED_NAME -> Comparator.comparing(item ->
                    ItemNameTranslator.getLocalizedName(item, player), String.CASE_INSENSITIVE_ORDER);
        };

        if (sortDirection == SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        items.sort(comparator);
    }
}
