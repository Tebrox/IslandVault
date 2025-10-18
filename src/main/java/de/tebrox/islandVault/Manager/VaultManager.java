package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.Events.VaultUpdateEvent;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Menu.VaultMenu;
import de.tebrox.islandVault.Utils.IslandVaultData;
import de.tebrox.islandVault.Utils.ItemNameTranslator;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Verantwortlich für das Laden, Speichern und Bereitstellen
 * der Insel-Vaults.
 *
 * Jede Insel hat ein gemeinsames Inventar, das durch Spielerrechte
 * (LuckPerms) gefiltert wird.
 */
public class VaultManager {

    private final Map<String, IslandVaultData> islandVaults = new ConcurrentHashMap<>();
    private final Map<String, Set<VaultMenu>> openMenus = new ConcurrentHashMap<>();
    private final IslandVault plugin;

    public VaultManager(IslandVault plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    // ===================== LADEN / SPEICHERN ====================
    // ============================================================

    public IslandVaultData loadIslandVault(String islandId) {
        File file = new File(plugin.getDataFolder(), "vaults/island_" + islandId + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            return createEmptyVault(islandId, file);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        IslandVaultData vault = new IslandVaultData(islandId);

        ConfigurationSection section = cfg.getConfigurationSection("Inventory");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                vault.getInventory().put(key, cfg.getInt("Inventory." + key, 0));
            }
        }

        vault.setAutoCollect(cfg.getBoolean("Island.Autocollect", false));
        vault.setShowAutocollectMessage(cfg.getBoolean("Island.ShowAutocollectMessage", false));

        // Backpack laden
        if (cfg.contains("Backpack")) {
            for (String slotKey : cfg.getConfigurationSection("Backpack").getKeys(false)) {
                int slot = Integer.parseInt(slotKey);
                ItemStack item = cfg.getItemStack("Backpack." + slotKey);
                vault.getBackpack().setItem(slot, item);
            }
        }

        islandVaults.put(islandId, vault);
        return vault;
    }

    private IslandVaultData createEmptyVault(String islandId, File file) {
        IslandVaultData vault = new IslandVaultData(islandId);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        cfg.set("Island.id", islandId.toString());
        cfg.set("Island.Autocollect", false);
        cfg.set("Island.ShowAutocollectMessage", false);
        saveIslandVault(vault);

        return vault;
    }

    public void saveIslandVault(IslandVaultData vault) {
        File file = new File(plugin.getDataFolder(), "vaults/island_" + vault.getIslandId() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        cfg.set("Island.id", vault.getIslandId().toString());
        cfg.set("Island.Autocollect", vault.isAutoCollectEnabled());
        cfg.set("Island.ShowAutocollectMessage", vault.isShowAutocollectMessage());

        cfg.set("Inventory", null);
        for (Map.Entry<String, Integer> entry : vault.getInventory().entrySet()) {
            cfg.set("Inventory." + entry.getKey(), entry.getValue());
        }

        cfg.set("Backpack", null);
        for (int i = 0; i < vault.getBackpack().getSize(); i++) {
            ItemStack item = vault.getBackpack().getItem(i);
            if (item != null) cfg.set("Backpack." + i, item);
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Speichern von " + file.getName(), e);
        }
    }

    // ============================================================
    // =============== GET / FILTER FÜR SPIELER ===================
    // ============================================================

    /**
     * Gibt alle Items zurück, die ein Spieler auf einer Insel sehen darf.
     */
    public List<ItemStack> getVisibleItemsForPlayer(Player player, String islandId) {
        IslandVaultData vault = islandVaults.computeIfAbsent(islandId, this::loadIslandVault);
        List<ItemStack> items = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : vault.getInventory().entrySet()) {
            String key = entry.getKey();
            int amount = entry.getValue();

            // Nur anzeigen, wenn Spieler die Permission hat
            if (!LuckPermsUtils.hasPermission(player.getUniqueId(), key)) continue;

            ItemStack base = IslandVault.getItemManager().getItemList().stream()
                    .filter(i -> IslandVault.getItemManager().buildPermissionKey(i).equalsIgnoreCase(key))
                    .findFirst().orElse(null);
            if (base == null) continue;

            ItemStack item = base.clone();
            ItemMeta meta = item.getItemMeta();

            meta.setLore(IslandVault.getLanguageManager().translateList(
                    player,
                    "menu.item.vaultItem.itemLore",
                    Map.of("amount", String.valueOf(amount),
                            "maxStackSize", String.valueOf(item.getMaxStackSize())),
                    true
            ));
            item.setItemMeta(meta);
            items.add(item);
        }

        return items;
    }

    // ============================================================
    // ==================== MENÜ HANDLING ==========================
    // ============================================================

    public void registerViewer(Player player, String islandId, VaultMenu menu) {
        openMenus.computeIfAbsent(islandId, k -> ConcurrentHashMap.newKeySet()).add(menu);
    }

    public void unregisterViewer(Player player, String islandId) {
        Set<VaultMenu> menus = openMenus.get(islandId);
        if (menus != null) {
            menus.removeIf(menu -> menu.getViewer().getUniqueId().equals(player.getUniqueId()));
            if (menus.isEmpty()) openMenus.remove(islandId);
        }
    }

    public void updateViewers(String islandId) {
        Set<VaultMenu> menus = openMenus.get(islandId);
        if (menus != null) {
            for (VaultMenu menu : menus) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    menu.refreshData();
                    menu.getViewer().updateInventory();
                });
            }
        }
    }

    public boolean hasViewers(String islandID) {
        return openMenus.containsKey(islandID);
    }

    public Map<String, IslandVaultData> getIslandVaults() {
        return islandVaults;
    }

// ============================================================
// ============   ITEM-EINLAGERUNG FÜR INSEL-VAULT   ==========
// ============================================================

    public boolean addItemToIslandVault(String permissionKey, int amount, String islandId, Player actor) {
        if (amount <= 0 || permissionKey == null || islandId == null) return false;

        // Prüfen ob Spieler die Berechtigung hat
        if (actor != null && !LuckPermsUtils.hasPermission(actor.getUniqueId(), permissionKey)) {
            return false;
        }

        IslandVaultData vault = islandVaults.computeIfAbsent(islandId, this::loadIslandVault);


        // Bestand erhöhen
        int current = vault.getInventory().getOrDefault(permissionKey, 0);
        vault.getInventory().put(permissionKey, current + amount);

        updateViewers(islandId);
        return true;
    }

    public boolean addItemToIslandVault(ItemStack stack, int amount, String islandId, Player actor) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        String key = IslandVault.getItemManager().buildPermissionKey(stack);
        return addItemToIslandVault(key, amount, islandId, actor);
    }

// ============================================================
// ============   ITEM-ENTNAHME FÜR INSEL-VAULT   =============
// ============================================================

    public @Nullable ItemStack takeItemFromIslandVault(String permissionKey, int amount, String islandId, Player actor) {
        if (amount <= 0 || permissionKey == null || islandId == null) return null;

        if (actor != null && !LuckPermsUtils.hasPermission(actor.getUniqueId(), permissionKey)) {
            return null;
        }

        IslandVaultData vault = islandVaults.computeIfAbsent(islandId, this::loadIslandVault);
        int current = vault.getInventory().getOrDefault(permissionKey, 0);
        if (current <= 0) return null;

        int toExtract = Math.min(current, amount);

        // Event
        //VaultUpdateEvent event = new VaultUpdateEvent(actor, islandId, permissionKey, -toExtract, false);
        //Bukkit.getPluginManager().callEvent(event);
        //if (event.isCancelled()) return null;

        // Bestand anpassen
        vault.getInventory().put(permissionKey, Math.max(0, current - toExtract));

        // Basis-Item finden
        ItemStack base = IslandVault.getItemManager().getItemList().stream()
                .filter(i -> IslandVault.getItemManager().buildPermissionKey(i).equalsIgnoreCase(permissionKey))
                .findFirst().orElse(null);

        if (base == null) return null;

        ItemStack result = base.clone();
        result.setAmount(toExtract);

        updateViewers(islandId);
        return result;
    }

    public @Nullable ItemStack takeItemFromIslandVault(ItemStack stack, int amount, String islandId, Player actor) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        String key = IslandVault.getItemManager().buildPermissionKey(stack);
        return takeItemFromIslandVault(key, amount, islandId, actor);
    }

    // ============================================================
    // ============   FILTER-METHODE (unverändert)   ==============
    // ============================================================

    public static List<ItemStack> filterItems(Player player, List<ItemStack> allItems, @Nullable String searchQuery) {
        if (allItems == null || allItems.isEmpty()) return Collections.emptyList();
        if (searchQuery == null || searchQuery.trim().isEmpty()) return new ArrayList<>(allItems);

        String needle = searchQuery.toLowerCase(Locale.ROOT);

        return allItems.stream()
                .filter(item -> {
                    try {
                        String name = ItemNameTranslator.getLocalizedName(item, player);
                        return name != null && name.toLowerCase(Locale.ROOT).contains(needle);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Prüft, ob für eine bestimmte Insel das AutoCollect-Feature aktiviert ist.
     *
     * @param islandId Die ID der Insel (String, BentoBox)
     * @return true, wenn AutoCollect aktiv ist, sonst false
     */
    public boolean autoCollectIsEnabled(String islandId) {
        if (islandId == null) return false;
        var vault = islandVaults.get(islandId);
        if (vault == null) return false;
        return vault.isAutoCollectEnabled();
    }

    /**
     * Prüft, ob Insel-Besitzer AutoCollect-Meldungen angezeigt bekommen darf.
     */
    public boolean islandOwnerCanSeeAutocollectMessage(String islandId) {
        if (islandId == null) return false;
        var vault = islandVaults.get(islandId);
        if (vault == null) return false;
        return vault.isShowAutocollectMessage();
    }

}
