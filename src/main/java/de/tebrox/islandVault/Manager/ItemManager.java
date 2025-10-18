package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.potion.PotionType;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;
import java.util.logging.Level;

public class ItemManager {

    private final IslandVault plugin;
    private final List<ItemStack> itemList = new ArrayList<>();

    private List<String> item_blacklist;
    private List<String> internalBlacklist;

    private static final String CACHE_FILE = "itemCache";

    public ItemManager(IslandVault plugin) {
        this.plugin = plugin;
        loadOrGenerateCache();
    }

    // ============================================================
    // =============== CACHE LADEN / GENERIEREN ===================
    // ============================================================

    @SuppressWarnings("unchecked")
    private void loadOrGenerateCache() {
        IslandVault.getConfigManager().loadRawConfig(CACHE_FILE);
        FileConfiguration cache = IslandVault.getConfigManager().getConfig(CACHE_FILE);

        if (cache.contains("items")) {
            Bukkit.getLogger().log(Level.INFO, "[ItemManager] Lade gecachten ItemCache...");
            try {
                List<ItemStack> cached = (List<ItemStack>) cache.get("items");
                if (cached != null && !cached.isEmpty()) {
                    itemList.clear();
                    itemList.addAll(cached);
                    Bukkit.getLogger().log(Level.INFO, "[ItemManager] " + itemList.size() + " Items aus Cache geladen.");
                    return;
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "[ItemManager] Fehler beim Lesen des Caches: " + e.getMessage());
            }
        }

        Bukkit.getLogger().log(Level.INFO, "[ItemManager] Erstelle neuen ItemCache...");
        reload();
        saveCache(cache);
    }

    private void saveCache(FileConfiguration cache) {
        cache.set("items", itemList);
        plugin.getConfigManager().saveConfig(CACHE_FILE);
        Bukkit.getLogger().log(Level.INFO, "[ItemManager] ItemCache gespeichert mit " + itemList.size() + " Items.");
    }

    // ============================================================
    // ===================== GENERIERUNG ==========================
    // ============================================================

    public void reload() {
        item_blacklist = plugin.getConfig().getStringList("blacklist");
        internalBlacklist = defaultInternalBlacklist();
        createItemList();
    }

    private List<String> defaultInternalBlacklist() {
        return new ArrayList<>(Arrays.asList(
                "shovel", "sword", "axe", "hoe",
                "helmet", "chestplate", "leggings", "boots",
                "bundle", "rod", "bow", "shulker",
                "written_book", "command", "structure"
        ));
    }

    private void createItemList() {
        itemList.clear();

        for (Material mat : Material.values()) {
            if (!mat.isAir() && !materialIsBlacklisted(mat) && !materialIsInternalBlacklisted(mat) && mat.isItem()) {
                addAllVariants(mat);
            }
        }

        Bukkit.getLogger().log(Level.INFO, "[ItemManager] Es wurden " + itemList.size() + " Items generiert.");
    }

    // ============================================================
    // ================== ITEMVARIANTEN ===========================
    // ============================================================

    private void addAllVariants(Material mat) {
        // 1️⃣ Basis-Item immer hinzufügen
        itemList.add(new ItemStack(mat));

        // 2️⃣ Tränke (alle Typen)
        if (mat == Material.POTION || mat == Material.SPLASH_POTION || mat == Material.LINGERING_POTION) {
            for (PotionType type : PotionType.values()) {
                ItemStack potion = new ItemStack(mat);
                potion.editMeta(meta -> {
                    if (meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
                        potionMeta.setBasePotionType(type);
                    }
                });
                itemList.add(potion);
            }
        }

        // 3️⃣ Ziegenhörner (8 Varianten)
        if (mat == Material.GOAT_HORN) {
            for (int i = 0; i < 8; i++) {
                ItemStack horn = new ItemStack(mat);
                int finalI = i;
                horn.editMeta(meta -> meta.setCustomModelData(finalI));
                itemList.add(horn);
            }
        }

        // 4️⃣ Bannerfarben
        if (mat.name().endsWith("_BANNER")) {
            for (DyeColor color : DyeColor.values()) {
                Material bannerMat = Material.matchMaterial(color.name() + "_BANNER");
                if (bannerMat != null) {
                    itemList.add(new ItemStack(bannerMat));
                }
            }
        }

        // 5️⃣ Bettenfarben
        if (mat.name().endsWith("_BED")) {
            for (DyeColor color : DyeColor.values()) {
                Material bedMat = Material.matchMaterial(color.name() + "_BED");
                if (bedMat != null) {
                    itemList.add(new ItemStack(bedMat));
                }
            }
        }

        // 6️⃣ Kerzenfarben
        if (mat.name().endsWith("_CANDLE")) {
            for (DyeColor color : DyeColor.values()) {
                Material candleMat = Material.matchMaterial(color.name() + "_CANDLE");
                if (candleMat != null) {
                    itemList.add(new ItemStack(candleMat));
                }
            }
        }

        // 7️⃣ Schallplatten
        if (mat.name().startsWith("MUSIC_DISC_")) {
            itemList.add(new ItemStack(mat));
        }

        // 8️⃣ Schilder (alle Holzsorten)
        if (mat.name().endsWith("_SIGN")) {
            for (Material m : Material.values()) {
                if (m.name().endsWith("_SIGN")) {
                    itemList.add(new ItemStack(m));
                }
            }
        }
    }

    // ============================================================
    // ====================== BLACKLIST ===========================
    // ============================================================

    public boolean materialIsBlacklisted(Material material) {
        for (String b : getItemBlacklist()) {
            if (material.toString().toLowerCase().contains(b.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean materialIsInternalBlacklisted(Material material) {
        for (String b : internalBlacklist) {
            if (material.toString().toLowerCase().contains(b.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // ====================== GETTER ==============================
    // ============================================================

    public List<String> getItemBlacklist() {
        return item_blacklist;
    }

    public List<ItemStack> getItemList() {
        return itemList;
    }

    public String buildPermissionKey(ItemStack item) {
        String base = "islandvault.item.";
        String key = item.getType().name().toLowerCase();

        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta pm && pm.getBasePotionType() != null) {
            key += "." + pm.getBasePotionType().name().toLowerCase();
        } else if (item.getItemMeta() instanceof MusicInstrumentMeta im && im.getInstrument() != null) {
            key += "." + im.getInstrument().getSound().toString();
        } else if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            key += ".cmd" + item.getItemMeta().getCustomModelData();
        }

        return base + key;
    }
}
