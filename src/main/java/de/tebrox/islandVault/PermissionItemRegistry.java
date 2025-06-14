package de.tebrox.islandVault;

import de.tebrox.islandVault.Utils.ItemStackKey;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import de.tebrox.islandVault.Utils.PermissionUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class PermissionItemRegistry {

    private final Plugin plugin;
    private final Map<String, ItemPermissionRule> permissionItemMap = new HashMap<>();
    private Set<String> blacklistIds = new HashSet<>();

    public PermissionItemRegistry(Plugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        loadBlacklist();
        permissionItemMap.clear();

        registerVanillaMaterials();
        registerGoatHorns();
        registerEnchantedBooks();
        registerCustomItems(); // Optional

        registerPermissions();
    }

    private void registerPermissions() {
        for(String s : permissionItemMap.keySet()) {
            PermissionUtils.registerPermission(s, "", PermissionDefault.FALSE);
        }
    }

    private void loadBlacklist() {
        File file = new File(plugin.getDataFolder(), "blacklist.yml");
        if (!file.exists()) {
            plugin.saveResource("blacklist.yml", false); // legt Standarddatei an
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        blacklistIds = new HashSet<>(config.getStringList("blacklist"));
    }

    private void registerVanillaMaterials() {
        for (Material material : Material.values()) {
            if (!material.isItem()) continue;
            ItemStack item = new ItemStack(material);
            String id = material.name().toLowerCase();

            ItemPermissionRule.RuleType type = blacklistIds.contains(id) ? ItemPermissionRule.RuleType.BLACKLIST : ItemPermissionRule.RuleType.WHITELIST;
            permissionItemMap.put(id, new ItemPermissionRule(ItemStackKey.of(item), type));
        }
    }

    private void registerGoatHorns() {
        List<String> sounds = Arrays.asList("ponder", "sing", "seek", "feel", "admire", "call", "yearn", "dream");
        for (String sound : sounds) {
            ItemStack horn = new ItemStack(Material.GOAT_HORN);
            horn.editMeta(meta -> meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "goat_sound"), PersistentDataType.STRING, sound
            ));
            String id = "goat_horn." + sound;
            ItemPermissionRule.RuleType type = blacklistIds.contains(id) ? ItemPermissionRule.RuleType.BLACKLIST : ItemPermissionRule.RuleType.WHITELIST;
            permissionItemMap.put(id, new ItemPermissionRule(ItemStackKey.of(horn), type));
        }
    }

    private void registerEnchantedBooks() {
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment == null || enchantment.getKey() == null) continue;

            for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                if (meta == null) continue;

                meta.addStoredEnchant(enchantment, level, true);
                book.setItemMeta(meta);

                String id = "enchanted_book." + enchantment.getKey().getKey().toLowerCase() + "_" + level;
                ItemPermissionRule.RuleType type = blacklistIds.contains(id) ? ItemPermissionRule.RuleType.BLACKLIST : ItemPermissionRule.RuleType.WHITELIST;
                permissionItemMap.put(id, new ItemPermissionRule(ItemStackKey.of(book), type));
            }
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
                .filter(entry -> entry.getValue().getKey().equals(key))
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

    public boolean isBlacklisted(ItemStackKey key) {
        return permissionItemMap.values().stream()
                .anyMatch(rule -> rule.getKey().equals(key) && rule.getType() == ItemPermissionRule.RuleType.BLACKLIST);
    }
}