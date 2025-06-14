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

    public boolean add(ItemStack stack, int amount) {
        if (amount <= 0) return false; // Kein Hinzufügen von null oder negativen Mengen

        ItemStackKey key = ItemStackKey.of(stack);
        int current = items.getOrDefault(key, 0);
        setAmount(stack, current + amount);
        return true;
    }

    public boolean remove(ItemStack stack, int amount) {
        if (amount <= 0) return false; // Kein Entfernen von null oder negativen Mengen

        ItemStackKey key = ItemStackKey.of(stack);
        int current = items.getOrDefault(key, 0);
        if (current < amount) return false; // Nicht genügend Items vorhanden

        setAmount(stack, current - amount);
        return true;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<ItemStack> filterItems(Player player, @Nullable String searchQuery) {
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

    public List<ItemStack> getAllItems() {
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<ItemStackKey, Integer> entry : items.entrySet()) {
            ItemStackKey key = entry.getKey();
            long amount = entry.getValue();

            if (amount <= 0) continue;

            ItemStack base = key.toItemStack();
            while (amount > 0) {
                ItemStack copy = base.clone();
                int stackAmount = (int) Math.min(amount, base.getMaxStackSize());
                copy.setAmount(stackAmount);
                result.add(copy);
                amount -= stackAmount;
            }
        }
        return result;
    }

    public ItemStack extractItem(ItemStackKey key, int requestedAmount) {
        int currentAmount = items.getOrDefault(key, 0);
        if (currentAmount <= 0) {
            return null; // Nichts da
        }

        int takeAmount = Math.min(requestedAmount, currentAmount);
        int newAmount = currentAmount - takeAmount;

        if (newAmount > 0) {
            items.put(key, newAmount);
        } else {
            items.remove(key);
        }

        ItemStack item = Base64ItemSerializer.deserialize(key.toString());
        item.setAmount(takeAmount);
        return item;
    }
}