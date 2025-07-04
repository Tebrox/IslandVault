package de.tebrox.islandVault.Menu.Admin;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.ItemPermissionRule;
import de.tebrox.islandVault.Menu.SettingsMenu;
import de.tebrox.islandVault.Menu.VaultMenu;
import de.tebrox.islandVault.PermissionItemRegistry;
import de.tebrox.islandVault.Utils.*;
import de.tebrox.islandVault.VaultData;
import me.kodysimpson.simpapi.colors.ColorTranslator;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import me.kodysimpson.simpapi.menu.PaginatedMenu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The VaultMenu displays a paginated view of the player's island storage.
 * It supports sorting, navigation, and interaction with stored items.
 * This class extends SimpAPI's {@link PaginatedMenu}.
 */
public class BlacklistMenu extends PaginatedMenu {
    protected ItemStack FILLER_GLASS = makeItem(Material.BLACK_STAINED_GLASS_PANE, "", true, false);
    protected ItemStack NO_ITEMS = makeItem(Material.BARRIER, "You have no items", false, false);
    private final Player viewer;


    public BlacklistMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
        maxItemsPerPage = 45;
        this.viewer = playerMenuUtility.getOwner();
    }

    @Override
    public List<ItemStack> dataToItems() {
        Map<String, ItemPermissionRule> rules = IslandVault.getPermissionItemRegistry().getPermissionItemMap();
        List<ItemStack> items = new ArrayList<>();

        Player player = playerMenuUtility.getOwner();
        boolean isOp = player.isOp();

        for(Map.Entry<String, ItemPermissionRule> ruleSet : rules.entrySet()) {
            ItemPermissionRule rule = ruleSet.getValue();
            ItemStack item = rule.getKey().toItemStack();

            if(item == null || item.getType().isAir()) continue;

            ItemMeta meta = item.getItemMeta();
            if(meta == null) continue;

            String type = rule.getType() == ItemPermissionRule.RuleType.WHITELIST ? "&6Whitelist" : "&cBlacklist";
            List<String> lore = IslandVault.getLanguageManager().translateList(player, "menu.item.admin.blacklistItem.itemLore", Map.of("ruleType", type));

            if (isOp && IslandVault.getPlugin().isDebug()) {
                lore.add("§8§m------------------------");
                lore.add("§6§lDEBUG-INFO");
                lore.add("§7Material: §f" + item.getType());
                // weitere Debug-Infos ...
                lore.add("§8§m------------------------");
            }

            meta.setLore(lore);

            if(rule.getType() == ItemPermissionRule.RuleType.BLACKLIST) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);

            items.add(item);
        }

        int size = items.size();
        int rest = size % maxItemsPerPage;
        if(rest > 0) {
            int z = maxItemsPerPage - rest;
            for(int i = 0; i < z; i++) {
                items.add(FILLER_GLASS);
            }
        }

        return items;
    }
    @Override
    public @Nullable HashMap<Integer, ItemStack> getCustomMenuBorderItems() {
        return null;
    }

    @Override
    public String getMenuName() {
        return IslandVault.getLanguageManager().translate(player, "menu.admin.blacklist.title");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public boolean cancelAllClicks() {
        return true;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) throws MenuManagerNotSetupException, MenuManagerException {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != player.getInventory()) {
            int slot = event.getSlot();
            ItemStack currentItem = event.getCurrentItem();

            if (slot >= 45 && slot <= 54) {
                if (currentItem == null || currentItem.isSimilar(FILLER_GLASS)) return;

                switch (slot) {
                    case 48:
                        prevPage();
                        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                        break;

                    case 49:
                        player.closeInventory();
                        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                        break;

                    case 50:
                        nextPage();
                        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                        break;
                    default:
                }
            }else{
                ItemStack temp = currentItem.clone();
                ItemMeta meta = temp.getItemMeta();

                meta.removeEnchant(Enchantment.UNBREAKING);
                meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);

                temp.setItemMeta(meta);

                ItemStackKey key = ItemStackKey.of(temp);

                Optional<String> idOpt = IslandVault.getPermissionItemRegistry().getId(key);
                if (idOpt.isEmpty()) {
                    PluginLogger.debug("Kein ID-Eintrag für ItemKey gefunden: " + key);
                    return;
                }
                String id = idOpt.get();
                System.out.println("ID: " + id +  " / Blacklist: " + IslandVault.getPermissionItemRegistry().isBlacklisted(id));

                ItemPermissionRule.RuleType rule = IslandVault.getPermissionItemRegistry().isBlacklisted(key) ? ItemPermissionRule.RuleType.WHITELIST : ItemPermissionRule.RuleType.BLACKLIST;
                IslandVault.getPermissionItemRegistry().setItemRule(id, key, rule);

                PluginLogger.debug("Set rule from item: " + currentItem.getI18NDisplayName() + " to " + rule.toString().toLowerCase());
                refreshData();
            }
        }
    }


    /**
     * @param material    The material to base the ItemStack on
     * @param displayName The display name of the ItemStack
     * @param hideTooltip Hide the Tooltip of the ItemStack
     * @param isEnchanted Enchant the item
     * @param lore        The lore of the ItemStack, with the Strings being automatically color coded with ColorTranslator
     * @return The constructed ItemStack object
     */
    public ItemStack makeItem(Material material, String displayName, boolean hideTooltip, boolean isEnchanted, String... lore) {

        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(displayName);
        itemMeta.setHideTooltip(hideTooltip);

        if(isEnchanted) {
            itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        //Automatically translate color codes provided
        itemMeta.setLore(Arrays.stream(lore).map(ColorTranslator::translateColorCodes).collect(Collectors.toList()));
        item.setItemMeta(itemMeta);

        return item;
    }

    @Override
    protected void addMenuBorder() {
        String texture;

        if(getCurrentPage() > 1) {
            // Previous page button
            texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTE4NWM5N2RiYjgzNTNkZTY1MjY5OGQyNGI2NDMyN2I3OTNhM2YzMmE5OGJlNjdiNzE5ZmJlZGFiMzVlIn19fQ==";
            inventory.setItem(48, makeSkullItem(texture, IslandVault.getLanguageManager().translate(player, "menu.previousPage", Map.of("currentPage", java.lang.String.valueOf(getCurrentPage()), "previousPage", java.lang.String.valueOf(getCurrentPage() - 1), "nextPage", java.lang.String.valueOf(getCurrentPage() + 1), "maxPage", java.lang.String.valueOf(getTotalPages()))), false, false));
            //inventory.setItem(48, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.previousPage")), false, false));
        }else{
            inventory.setItem(48, FILLER_GLASS);
        }

        // Close button
        texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2FkNTMyYWRiYTliNGY5ZWZlYjQ3ZjRkNmI0ZmMxZWQyZTZkZTVjY2FhNzc5ZjU1NTk4ZTFlYWVjYWVlZjg5MiJ9fX0=";
        inventory.setItem(49, makeSkullItem(texture, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.close")), false, false));

        if(getCurrentPage() < getTotalPages()) {
            texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzFjMGVkZWRkNzExNWZjMWIyM2Q1MWNlOTY2MzU4YjI3MTk1ZGFmMjZlYmI2ZTQ1YTY2YzM0YzY5YzM0MDkxIn19fQ==";
            // Next page button
            inventory.setItem(50, makeSkullItem(texture, IslandVault.getLanguageManager().translate(player, "menu.nextPage", Map.of("currentPage", java.lang.String.valueOf(getCurrentPage()), "previousPage", java.lang.String.valueOf(getCurrentPage() - 1), "nextPage", java.lang.String.valueOf(getCurrentPage() + 1), "maxPage", java.lang.String.valueOf(getTotalPages()))), false, false));
        }else{
            inventory.setItem(50, FILLER_GLASS);

        }


        for (int i = 45; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, FILLER_GLASS);
            }
        }

        //place the custom items if they exist
        if (getCustomMenuBorderItems() != null) {
            getCustomMenuBorderItems().forEach((integer, itemStack) -> inventory.setItem(integer, itemStack));
        }
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();

        List<ItemStack> items = getItems(); // Use cached items

        int slot = 0;
        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= items.size()) break;

            inventory.setItem(slot, items.get(index));
            slot++;
        }
    }

    @Override
    public void open() {
        //The owner of the inventory created is the Menu itself,
        // so we are able to reverse engineer the Menu object from the
        // inventoryHolder in the MenuListener class when handling clicks
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());

        //grab all the items specified to be used for this menu and add to inventory
        this.setMenuItems();

        //open the inventory for the player
        player.openInventory(inventory);
        //playerMenuUtility.getOwner().openInventory(inventory);
        playerMenuUtility.pushMenu(this);
    }


    public Player getViewer() {
        return viewer;
    }

    @Override
    public void handleMenuClose() {

    }

    public ItemStack makeSkullItem(String texture, String displayName, boolean hideTooltip, boolean isEnchanted, String... lore) {
        ItemStack item = SkullCreator.fromBase64(texture);
        ItemMeta itemMeta = item.getItemMeta();

        itemMeta.setDisplayName(displayName);
        itemMeta.setHideTooltip(hideTooltip);

        if(isEnchanted) {
            itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        //Automatically translate color codes provided
        itemMeta.setLore(Arrays.stream(lore).map(ColorTranslator::translateColorCodes).collect(Collectors.toList()));
        item.setItemMeta(itemMeta);

        return item;
    }
}
