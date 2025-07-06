package de.tebrox.islandVault;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Utils.ItemStackKey;
import de.tebrox.islandVault.Utils.PermissionUtils;
import de.tebrox.islandVault.Utils.PluginLogger;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;
import world.bentobox.bentobox.database.objects.Island;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PermissionItemRegistry {

    private final Plugin plugin;
    private final Map<String, ItemPermissionRule> permissionItemMap = new LinkedHashMap<>();
    private Set<String> blacklistIds = new HashSet<>();
    private List<Material> hasSubMaterial = Arrays.asList(Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION, Material.TIPPED_ARROW, Material.GOAT_HORN, Material.ENCHANTED_BOOK);

    public PermissionItemRegistry(Plugin plugin) {
        this.plugin = plugin;
        System.out.println("sadjfsjöldaxjfölsajfökljdsaöfjdsfökljsaföklsajfsdfjsölkfjöksajfsaölkdjsökl");
        load();

        PluginLogger.info("Registrierte Items: " + permissionItemMap.size());
    }

    public void load() {
        loadBlacklist();
        permissionItemMap.clear();

        registerVanillaMaterials();
        registerGoatHorns();
        registerEnchantedBooks();
        registerVanillaPotions();
        registerCustomItems(); // Optional

        //registerPermissions();

        getPermissionItemMap().forEach((id, rule) -> {
            try {
                rule.getKey().toItemStack();
            } catch (Exception e) {
                PluginLogger.warning("Ungültiger gespeicherter ItemKey: " + id + " → " + e.getMessage());
            }
        });
    }

    private void registerPermissions() {
        for(String s : getPermissionItemMap().keySet()) {
            PluginLogger.debug("Registered Permission: " + s);
            PermissionUtils.registerPermission(s, "", PermissionDefault.FALSE);
        }
    }

    private void registerPermission(String id, String message) {
        PluginLogger.debug(message);
        String permission = Permissions.VAULT.getLabel() + id;
        PermissionUtils.registerPermission(permission, id, PermissionDefault.FALSE);
    }


    private void loadBlacklist() {
        File file = new File(plugin.getDataFolder(), "blacklist.yml");
        if (!file.exists()) {
            plugin.saveResource("blacklist.yml", false); // legt Standarddatei an
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        blacklistIds = new HashSet<>(config.getStringList("blacklist"));
    }

    private void saveBlacklist() {
        File file = new File(plugin.getDataFolder(), "blacklist.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("blacklist", new ArrayList<>(blacklistIds));
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Blacklist: " + e.getMessage());
        }
    }

    public void setItemRule(String id, ItemStackKey key, ItemPermissionRule.RuleType ruleType) {
        permissionItemMap.put(id, new ItemPermissionRule(id, key, ruleType));
        if (ruleType == ItemPermissionRule.RuleType.BLACKLIST) {
            blacklistIds.add(id);
            System.out.println("blacklist");
        } else {
            blacklistIds.remove(id);
            System.out.println("whitelist");
        }
        saveBlacklist();
    }

    private void registerVanillaMaterials() {
        for (Material material : Material.values()) {
            if (!material.isItem() || material.isAir() || hasSubMaterial.contains(material)) continue;
            ItemStack item = new ItemStack(material);

            String id = material.name().toLowerCase();
            ItemPermissionRule.RuleType type = blacklistIds.contains(id) ? ItemPermissionRule.RuleType.BLACKLIST : ItemPermissionRule.RuleType.WHITELIST;

            if (permissionItemMap.containsKey(id)) {
                PluginLogger.debug("Item-ID bereits vorhanden: " + id);
            } else {
                registerPermission(id, "Register item: " + id);
                permissionItemMap.put(id, new ItemPermissionRule(id, ItemStackKey.of(item), type));
            }
        }
    }

    private void registerGoatHorns() {
        for(MusicInstrument music : MusicInstrument.values()) {
            ItemStack horn = new ItemStack(Material.GOAT_HORN);
            MusicInstrumentMeta meta = (MusicInstrumentMeta) horn.getItemMeta();
            meta.setInstrument(music);
            horn.setItemMeta(meta);

            String id = "goat_horn." + meta.getInstrument().key().asMinimalString().split("_")[0];
            ItemPermissionRule.RuleType type = blacklistIds.contains(id) ? ItemPermissionRule.RuleType.BLACKLIST : ItemPermissionRule.RuleType.WHITELIST;

            if (permissionItemMap.containsKey(id)) {
                PluginLogger.warning("Goat-Horn-ID bereits vorhanden: " + id);
            } else {
                registerPermission(id, "Register goat horn: " + id);
                permissionItemMap.put(id, new ItemPermissionRule(id, ItemStackKey.of(horn), type));
            }
        }
    }


    private void registerEnchantedBooks() {
        for (Enchantment enchantment : Enchantment.values()) {
            for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                if (meta == null) continue;

                meta.addStoredEnchant(enchantment, level, true);
                book.setItemMeta(meta);

                String id = "enchanted_book." + enchantment.getKey().getKey().toLowerCase() + "_" + level;
                ItemPermissionRule.RuleType type = blacklistIds.contains(id) ? ItemPermissionRule.RuleType.BLACKLIST : ItemPermissionRule.RuleType.WHITELIST;

                if (permissionItemMap.containsKey(id)) {
                    PluginLogger.warning("Enchantedbook-ID bereits vorhanden: " + id);
                } else {
                    registerPermission(id, "Register enchanted book: " + id);
                    permissionItemMap.put(id, new ItemPermissionRule(id, ItemStackKey.of(book), type));
                }
            }
        }
    }

    private void registerVanillaPotions() {
        for (Material material : List.of(
                Material.POTION,
                Material.SPLASH_POTION,
                Material.LINGERING_POTION,
                Material.TIPPED_ARROW)) {

            for (PotionType type : PotionType.values()) {
                // Normale Variante
                registerPotionVariant(material, type, "");

                // Erweiterte (extended) Variante
                if (type.isExtendable()) {
                    registerPotionVariant(material, type, "_long");
                }

                // Verstärkte (upgraded) Variante
                if (type.isUpgradeable()) {
                    registerPotionVariant(material, type, "_strong");
                }
            }
        }
    }

    private void registerPotionVariant(Material material, PotionType potionType, String suffix) {
        ItemStack potion = new ItemStack(material);
        potion.editMeta(meta -> {
            if (meta instanceof PotionMeta potionMeta) {
                potionMeta.setBasePotionType(potionType);

                // Extended/Upgraded über separate Methoden (ältere APIs)
                // Für neuere Bukkit-Versionen (1.20.5+) ggf. per NBT direkt oder Utility
            }
        });

        String suffixId = suffix.isEmpty() ? "" : suffix.replace("_", ".");
        String id = material.name().toLowerCase(Locale.ROOT) + "." + potionType.name().toLowerCase(Locale.ROOT) + suffixId;

        ItemStackKey key = ItemStackKey.of(potion);
        ItemPermissionRule.RuleType type = blacklistIds.contains(id) ? ItemPermissionRule.RuleType.BLACKLIST : ItemPermissionRule.RuleType.WHITELIST;

        if (permissionItemMap.containsKey(id)) {
            PluginLogger.warning("Potion-ID bereits vorhanden: " + id);
        } else {
            registerPermission(id, "Register potion: " + id);
            permissionItemMap.put(id, new ItemPermissionRule(id, key, type));
        }

    }

    private void registerCustomItems() {
        // Example: ItemsAdder
        /*
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            for (CustomStack stack : CustomStack.getAllCustomStacks()) {
                ItemStack item = stack.getItemStack();
                String id = "itemsadder." + stack.getNamespacedID();
                ItemPermissionRule.RuleType type = blacklistIds.contains(id) ? ItemPermissionRule.RuleType.BLACKLIST : ItemPermissionRule.RuleType.WHITELIST;
                permissionItemMap.put(id, new ItemPermissionRule(ItemStackKey.of(item), type));
            }
        }
        */
    }

    public Map<String, ItemPermissionRule> getPermissionItemMap() {
        return Collections.unmodifiableMap(permissionItemMap);
    }

    public Optional<ItemPermissionRule> getById(String id) {
        return Optional.ofNullable(permissionItemMap.get(id));
    }

    public Optional<String> getId(ItemStackKey key) {
        return permissionItemMap.entrySet().stream()
                .filter(entry -> {
                    try {
                        ItemStack stored = entry.getValue().getKey().toItemStack();
                        ItemStack input = key.toItemStack();
                        return stored != null && input != null && stored.isSimilar(input);
                    } catch (Exception e) {
                        PluginLogger.warning("Fehler beim Vergleichen eines ItemStackKeys (" + entry.getKey() + "): " + e.getMessage());
                        return false;
                    }
                })
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public Map<String, ItemPermissionRule> getWhitelistedItems() {
        return permissionItemMap.entrySet().stream()
                .filter(entry -> entry.getValue().getType() == ItemPermissionRule.RuleType.WHITELIST)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, ItemPermissionRule> getBlacklistItems() {
        return permissionItemMap.entrySet().stream()
                .filter(entry -> entry.getValue().getType() == ItemPermissionRule.RuleType.BLACKLIST)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean isBlacklisted(String id) {
        return permissionItemMap.containsKey(id);
    }

    public boolean isBlacklisted(ItemStackKey key) {
        return permissionItemMap.values().stream()
                .anyMatch(rule -> rule.getKey().equals(key) && rule.getType() == ItemPermissionRule.RuleType.BLACKLIST);
    }

    public Set<ItemStackKey> getWhitelistedItemKeys() {
        return permissionItemMap.values().stream()
                .filter(rule -> rule.getType() == ItemPermissionRule.RuleType.WHITELIST)
                .map(ItemPermissionRule::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}