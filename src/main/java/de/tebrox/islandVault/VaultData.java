package de.tebrox.islandVault;

import de.tebrox.islandVault.Manager.ItemGroupManager;
import de.tebrox.islandVault.Utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class VaultData {

    private final String islandId;
    private final String ownerName;
    private final UUID ownerUUID;
    private boolean autoCollect;
    private int accessLevel;

    private final Map<ItemStackKey, Integer> items = new ConcurrentHashMap<>();

    public VaultData(String islandId, UUID ownerUUID) {
        this.islandId = islandId;
        this.ownerName = Bukkit.getOfflinePlayer(ownerUUID).getName();
        this.ownerUUID = ownerUUID;
        this.autoCollect = false;
        this.accessLevel = BentoBoxRanks.getId("owner");
    }

    public String getIslandId() { return islandId; }

    public String getOwnerName() { return ownerName; }
    public UUID getOwnerUUID() { return ownerUUID; }

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


    public boolean getAutoCollect() {
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
        if (amount <= 0) return false;
        ItemStackKey newKey = ItemStackKey.of(stack);
        String itemID = IslandVault.getPermissionItemRegistry().getId(newKey).orElse("");

        if (!hasPermissionForItemOrGroup(ownerUUID, itemID)) return false;

        // Prüfen, ob der Key laut Whitelist erlaubt ist
        Set<ItemStackKey> allowedKeys = IslandVault.getPermissionItemRegistry().getWhitelistedItemKeys();
        if (!allowedKeys.contains(newKey)) {
            return false;
        }

        int current = items.getOrDefault(newKey, 0);
        setAmount(stack, current + amount);
        return true;
    }

    public boolean remove(ItemStack stack, int amount) {
        if (amount <= 0) return false; // Kein Entfernen von null oder negativen Mengen

        ItemStackKey key = ItemStackKey.of(stack);
        String itemID = IslandVault.getPermissionItemRegistry().getId(key).orElse("");

        if(!hasPermissionForItemOrGroup(ownerUUID, itemID)) return false;

        int current = items.getOrDefault(key, 0);
        if (current < amount) return false; // Nicht genügend Items vorhanden

        setAmount(stack, current - amount);
        return true;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

//    public List<ItemStack> filterItems(Player player, @Nullable String searchQuery) {
//        if (searchQuery == null || searchQuery.trim().isEmpty()) {
//            Map<ItemStackKey, Integer> combined = items;
//
//            if(!PlayerDataUtils.loadShowOnlyItemsWithAmount(player)) {
//                combined = getCombinedItemMap(items, IslandVault.getPermissionItemRegistry().getWhitelistedItems());
//            }
//
//            return combined.keySet().stream()
//                    .map(ItemStackKey::toItemStack)
//                    .collect(Collectors.toList());
//        }
//
//        String lower = searchQuery.toLowerCase(Locale.ROOT);
//
//        Map<ItemStackKey, Integer> combined = items;
//
//        if(!PlayerDataUtils.loadShowOnlyItemsWithAmount(player)) {
//            combined = getCombinedItemMap(items, IslandVault.getPermissionItemRegistry().getWhitelistedItems());
//        }
//
//        return combined.keySet().stream()
//                .map(ItemStackKey::toItemStack)
//                .filter(item -> {
//                    if (item == null || item.getType().isAir()) return false;
//
//                    // Name prüfen
//                    String name = ItemNameTranslator.getLocalizedName(item, player).toLowerCase(Locale.ROOT);
//                    if (name.contains(lower)) return true;
//
//                    // Lore prüfen
//                    ItemMeta meta = item.getItemMeta();
//                    if (meta != null && meta.hasLore()) {
//                        for (String line : meta.getLore()) {
//                            if (line != null && line.toLowerCase(Locale.ROOT).contains(lower)) {
//                                return true;
//                            }
//                        }
//                    }
//
//                    return false;
//                })
//                .collect(Collectors.toList());
//    }

    public List<ItemStack> filterItems(Player player, @Nullable String searchQuery) {
        UUID uuid = player.getUniqueId();
        PermissionItemRegistry registry = IslandVault.getPermissionItemRegistry();

        // Entscheide, ob Whitelist-Filterung aktiviert ist
        Map<ItemStackKey, Integer> combined = PlayerDataUtils.loadShowOnlyItemsWithAmount(player)
                ? items
                : getCombinedItemMap(items, registry.getWhitelistedItems());

        boolean hasSearch = searchQuery != null && !searchQuery.trim().isEmpty();
        String lower = hasSearch ? searchQuery.toLowerCase(Locale.ROOT) : null;

        return combined.keySet().stream()
                .map(ItemStackKey::toItemStack)
                .filter(item -> {
                    if (item == null || item.getType().isAir()) return false;

                    // Blacklist-Check
                    if (registry.isBlacklisted(ItemStackKey.of(item))) return false;

                    // Berechtigungs-Check
                    if (!hasPermissionForItemOrGroup(uuid, registry.getId(ItemStackKey.of(item)).orElse(""))) return false;

                    // Ohne Suchbegriff: alle zugelassenen Items
                    if (!hasSearch) return true;

                    // Mit Suchbegriff: prüfe Name und Lore
                    String name = ItemNameTranslator.getLocalizedName(item, player).toLowerCase(Locale.ROOT);
                    if (name.contains(lower)) return true;

                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        return meta.getLore().stream()
                                .filter(Objects::nonNull)
                                .map(line -> line.toLowerCase(Locale.ROOT))
                                .anyMatch(line -> line.contains(lower));
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

    private Map<ItemStackKey, Integer> getCombinedItemMap(Map<ItemStackKey, Integer> items, Map<String, ItemPermissionRule> permissionItemMap) {
        Map<ItemStackKey, Integer> combined = new HashMap<>(items);

        permissionItemMap.values().stream()
                .filter(rule -> rule.getType() == ItemPermissionRule.RuleType.WHITELIST)
                .map(ItemPermissionRule::getKey)
                .filter(key -> !combined.containsKey(key)) // Nur hinzufügen, wenn noch nicht vorhanden
                .forEach(key -> combined.put(key, 0));     // Menge 0 als Platzhalter

        return combined;
    }

    /**
     * Prüft, ob der Spieler die Permission für das Item oder eine Gruppe hat, in der das Item enthalten ist.
     */
    private boolean hasPermissionForItemOrGroup(UUID uuid, String id) {
        // Einzelitem-Permission prüfen
        if (LuckPermsUtils.hasPermissionForItem(uuid, id)) {
            return true;
        }

        // Gruppen-Permissions prüfen
        List<String> groups = ItemGroupManager.findGroupsWithItemID(id);
        for (String group : groups) {
            if (LuckPermsUtils.hasPermissionForGroup(uuid, group)) {
                return true;
            }
        }

        return false;
    }
}