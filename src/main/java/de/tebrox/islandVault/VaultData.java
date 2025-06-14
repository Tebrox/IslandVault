package de.tebrox.islandVault;

import de.tebrox.islandVault.Utils.BentoBoxRanks;
import de.tebrox.islandVault.Utils.ItemNameTranslator;
import de.tebrox.islandVault.Utils.ItemStackKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class VaultData {

    private final String islandId;
    private final String ownerName;
    private boolean autoCollect;
    private int accessLevel;

    private final Map<ItemStackKey, Integer> items = new ConcurrentHashMap<>();

    public VaultData(String islandId, String ownerName) {
        this.islandId = islandId;
        this.ownerName = ownerName;
        this.autoCollect = false;
        this.accessLevel = BentoBoxRanks.getId("owner");
    }

    public String getIslandId() { return islandId; }

    public String getOwnerName() { return ownerName; }

    public Map<ItemStackKey, Integer> getItems() { return items; }

    public void setAmount(ItemStack stack, int amount) {
        ItemStackKey key = ItemStackKey.of(stack);
        if (amount > 0) {
            items.put(key, amount);
        } else {
            items.remove(key);
        }
    }

    public int getAmount(ItemStack stack) {
        return items.getOrDefault(ItemStackKey.of(stack), 0);
    }


    public boolean isAutoCollect() {
        return autoCollect;
    }

    public void setAutoCollect(boolean autoCollect) {
        this.autoCollect = autoCollect;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    public void setAccessLevel(String roleName) {
        this.accessLevel = BentoBoxRanks.getId(roleName);
    }

    public void add(ItemStack stack, int amount) {
        ItemStackKey key = ItemStackKey.of(stack);
        int current = items.getOrDefault(key, 0);
        setAmount(stack, current + amount);
    }

    public void remove(ItemStack stack, int amount) {
        add(stack, -amount);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public static List<ItemStack> filterItems(Player player, @Nullable String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            // Alle Items zurückgeben (nur Keys in ItemStacks umwandeln)
            return items.keySet().stream()
                    .map(ItemStackKey::toItemStack)
                    .collect(Collectors.toList());
        }

        String lower = searchQuery.toLowerCase(Locale.ROOT);

        return items.keySet().stream()
                .map(ItemStackKey::toItemStack)
                .filter(item -> {
                    if (item == null || item.getType().isAir()) return false;

                    // Name prüfen
                    String name = ItemNameTranslator.getLocalizedName(item, player).toLowerCase(Locale.ROOT);
                    if (name.contains(lower)) return true;

                    // Lore prüfen
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        for (String line : meta.getLore()) {
                            if (line != null && line.toLowerCase(Locale.ROOT).contains(lower)) {
                                return true;
                            }
                        }
                    }

                    return false;
                })
                .collect(Collectors.toList());
    }
}