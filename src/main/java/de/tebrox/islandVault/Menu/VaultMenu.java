package de.tebrox.islandVault.Menu;

import de.tebrox.islandVault.IslandVault;
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
public class VaultMenu extends PaginatedMenu {
    protected ItemStack FILLER_GLASS = makeItem(Material.BLACK_STAINED_GLASS_PANE, "", true, false);
    protected ItemStack NO_ITEMS = makeItem(Material.BARRIER, "You have no items", false, false);
    private UUID ownerUUID = null;

    private ItemSortUtil.SortOption currentOption;
    private ItemSortUtil.SortDirection currentDirection;
    private final List<ItemSortUtil.SortOption> options = Arrays.asList(ItemSortUtil.SortOption.values());
    private final Island island;
    private final VaultData vaultData;

    private final Player viewer;


    public VaultMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
        maxItemsPerPage = 45;
        this.viewer = playerMenuUtility.getOwner();

        island = BentoBox.getInstance().getIslands().getIslandAt(viewer.getLocation()).orElse(null);
        IslandVault.getVaultSyncManager().registerViewer(island.getUniqueId(), viewer, this);

        this.vaultData = IslandVault.getVaultManager().getCachedVault(island.getUniqueId());
        this.ownerUUID = vaultData.getOwnerUUID();

    }

    @Override
    public List<ItemStack> dataToItems() {
        String searchQuery = "";

        if(playerMenuUtility.getData("searchQuery") != null) {
            searchQuery = String.valueOf(playerMenuUtility.getData("searchQuery"));
        }


        if(ownerUUID == null) {
            List<ItemStack> temp = new ArrayList<>();
            temp.add(NO_ITEMS);
            for(int i = 1; i < maxItemsPerPage; i++) {
                temp.add(FILLER_GLASS);
            }
            return temp;
        }

        currentOption = PlayerDataUtils.loadSortOption(playerMenuUtility.getOwner());
        currentDirection = PlayerDataUtils.loadSortDirection(playerMenuUtility.getOwner());
        List<ItemStack> filteredItems = vaultData.filterItems(playerMenuUtility.getOwner(), searchQuery);

        ItemSortUtil.sortItems(filteredItems, vaultData, player, currentOption, currentDirection);

        Player player = playerMenuUtility.getOwner();
        boolean isOp = player.isOp();

        for (int i = 0; i < filteredItems.size(); i++) {
            ItemStack item = filteredItems.get(i);
            if (item == null || item.getType().isAir()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            lore = IslandVault.getLanguageManager().translateList(player, "menu.item.vaultItem.itemLore", Map.of("amount", String.valueOf(vaultData.getAmount(item)), "maxStackSize", String.valueOf(item.getMaxStackSize())), true);

            // Zusätzliche OP-Lore, falls OP
            if (isOp) {
                lore.add("§8§m------------------------");
                lore.add("§6§lDEBUG-INFO");
                lore.add("§7Material: §f" + item.getType());
                // weitere Debug-Infos ...
                lore.add("§8§m------------------------");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            filteredItems.set(i, item);
        }

        int size = filteredItems.size();
        int rest = size % maxItemsPerPage;
        if(rest > 0) {
            int z = maxItemsPerPage - rest;
            for(int i = 0; i < z; i++) {
                filteredItems.add(FILLER_GLASS);
            }
        }
        return filteredItems;
    }
    @Override
    public @Nullable HashMap<Integer, ItemStack> getCustomMenuBorderItems() {
        return null;
    }

    @Override
    public String getMenuName() {
        return IslandVault.getLanguageManager().translate(player, "menu.title");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public boolean cancelAllClicks() {
        return false;
    }

    @Override
    public void handleMenu(InventoryClickEvent inventoryClickEvent) throws MenuManagerNotSetupException, MenuManagerException {
        if(inventoryClickEvent.getWhoClicked() instanceof Player player) {
            Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
            if(island == null) return;

            if(inventoryClickEvent.getClickedInventory() == player.getInventory()) {
                int slot = inventoryClickEvent.getSlot();
                ItemStack item = inventoryClickEvent.getCurrentItem();
                if(inventoryClickEvent.isShiftClick()) {
                    inventoryClickEvent.setCancelled(true);
                    if(inventoryClickEvent.isLeftClick()) {
                        if(vaultData.add(item, item.getAmount())) {
                            refreshData();
                            player.getInventory().setItem(slot, null);
                        }
                    }else if(inventoryClickEvent.isRightClick()) {
                        if(vaultData.add(item, item.getAmount())) {
                            refreshData();
                            ItemStack tempItem = item.clone();
                            tempItem.setAmount(tempItem.getAmount() - 1);
                            player.getInventory().setItem(slot, tempItem);
                        }
                    }
                }
            }

            if(inventoryClickEvent.getClickedInventory() != player.getInventory()) {
                int slot = inventoryClickEvent.getSlot();
                ItemStack cursor = inventoryClickEvent.getCursor();
                ItemStack item = inventoryClickEvent.getCurrentItem();


                if(slot >= 45 && slot <= 54) {
                    inventoryClickEvent.setCancelled(true);
                    switch (slot) {
                        case 45:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                if(inventoryClickEvent.isLeftClick()) {
                                    int nextIndex = (options.indexOf(currentOption) + 1) % options.size();
                                    currentOption = options.get(nextIndex);
                                    PlayerDataUtils.saveSortSettings(playerMenuUtility.getOwner(), currentOption, currentDirection);
                                }else if(inventoryClickEvent.isRightClick()) {
                                    currentDirection = (currentDirection == ItemSortUtil.SortDirection.ASCENDING)
                                            ? ItemSortUtil.SortDirection.DESCENDING
                                            : ItemSortUtil.SortDirection.ASCENDING;
                                    PlayerDataUtils.saveSortSettings(playerMenuUtility.getOwner(), currentOption, currentDirection);
                                }
                                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                                refreshData();
                            }
                            break;
                        case 47:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                firstPage();
                                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                            }
                            break;
                        case 48:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                prevPage();
                                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                            }
                            break;
                        case 49:
                            player.closeInventory();
                            player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                            break;
                        case 50:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                nextPage();
                                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                            }
                            break;
                        case 51:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                lastPage();
                                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                            }
                            break;
                        case 53:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                MenuManager.getPlayerMenuUtility(player).setData("previousMenu", VaultMenu.class);
                                MenuManager.getPlayerMenuUtility(player).setData("vaultData", vaultData);
                                MenuManager.openMenu(SettingsMenu.class, player);
                                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                            }
                    }
                }

                //Vault Slots
                else {
                    inventoryClickEvent.setCancelled(true);
                    boolean isLeftClick = inventoryClickEvent.isLeftClick();
                    boolean isRightClick = inventoryClickEvent.isRightClick();
                    boolean isShiftClick = inventoryClickEvent.isShiftClick();

                    if(cursor != null && cursor.getType() == item.getType() && !isShiftClick && isRightClick) {
                        int cursorAmount = cursor.getAmount();

                        ItemStack tempItem = vaultData.extractItem(ItemStackKey.of(cursor), 1);
                        if(tempItem != null) {
                            ItemStack handItem = new ItemStack(cursor.getType(), cursorAmount + tempItem.getAmount());
                            player.setItemOnCursor(handItem);

                            refreshData();
                        }
                    } else if (cursor == null || cursor.getType() == Material.AIR && item != null) { //taking item out of dsu

                        ItemStack handItem;
                        int amount = 0;
                        if(isLeftClick) {
                            amount = item.getMaxStackSize();
                        }else if (isRightClick) {
                            amount = 1;
                        }
                        handItem = vaultData.extractItem(ItemStackKey.of(item), amount);

                        if(handItem == null) return;

                        if(!isShiftClick) {
                            player.setItemOnCursor(handItem);
                        }else{
                            if(player.getInventory().firstEmpty() != -1){
                                player.getInventory().addItem(handItem);
                            }
                        }
                        refreshData();

                    } else if (cursor != null && cursor.getType() != Material.AIR) {
                        Material material = cursor.getType();
                        int amount = cursor.getAmount();

                        if(vaultData.add(item, amount)) {
                            refreshData();
                            player.setItemOnCursor(null);
                        }
                    }
                }
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

    public ItemStack makeSortItem(Material material) {
        currentOption = PlayerDataUtils.loadSortOption(playerMenuUtility.getOwner());
        currentDirection = PlayerDataUtils.loadSortDirection(playerMenuUtility.getOwner());
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();

        String label = switch (currentOption) {
            case ID -> IslandVault.getLanguageManager().translate(playerMenuUtility.getOwner(), "menu.item.sort.options.id");
            case AMOUNT -> IslandVault.getLanguageManager().translate(playerMenuUtility.getOwner(), "menu.item.sort.options.amount");
            case LOCALIZED_NAME -> IslandVault.getLanguageManager().translate(playerMenuUtility.getOwner(), "menu.item.sort.options.name");
        };
        String arrow = currentDirection == ItemSortUtil.SortDirection.ASCENDING ? "⬆" : "⬇";

        itemMeta.setLore(IslandVault.getLanguageManager().translateList(playerMenuUtility.getOwner(), "menu.item.sort.itemLore"));
        itemMeta.setDisplayName(ChatColor.YELLOW + label + " " + arrow);
        item.setItemMeta(itemMeta);

        return item;
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

    @Override
    protected void addMenuBorder() {
        String texture;

        inventory.setItem(45, makeSortItem(Material.COMPASS));

        if(getCurrentPage() > 1) {
            // First page button
            inventory.setItem(47, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.firstPage")), false, true));
            // Previous page button
            texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTE4NWM5N2RiYjgzNTNkZTY1MjY5OGQyNGI2NDMyN2I3OTNhM2YzMmE5OGJlNjdiNzE5ZmJlZGFiMzVlIn19fQ==";
            inventory.setItem(48, makeSkullItem(texture, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.previousPage")), false, false));
            //inventory.setItem(48, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.previousPage")), false, false));
        }else{
            inventory.setItem(47, FILLER_GLASS);
            inventory.setItem(48, FILLER_GLASS);
        }

        if(getCurrentPage() < getTotalPages()) {
            texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzFjMGVkZWRkNzExNWZjMWIyM2Q1MWNlOTY2MzU4YjI3MTk1ZGFmMjZlYmI2ZTQ1YTY2YzM0YzY5YzM0MDkxIn19fQ==";
            // Next page button
            inventory.setItem(50, makeSkullItem(texture, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.nextPage")), false, false));
            // Last page button
            inventory.setItem(51, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.lastPage")), false, true));
        }else{
            inventory.setItem(50, FILLER_GLASS);
            inventory.setItem(51, FILLER_GLASS);
        }

        // Close button
        texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2FkNTMyYWRiYTliNGY5ZWZlYjQ3ZjRkNmI0ZmMxZWQyZTZkZTVjY2FhNzc5ZjU1NTk4ZTFlYWVjYWVlZjg5MiJ9fX0=";
        inventory.setItem(49, makeSkullItem(texture, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.close")), false, false));

        if(ownerUUID.equals(viewer.getUniqueId())) {
            texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQyNzM3YThkMTlhYjA3OWNlZjdhODI0ZjE0OTcxNTVkYWE5MjVlOGMxYzZiN2E2Yzc1NjVlOGJlMzAzOWZhMCJ9fX0=";
            inventory.setItem(53, makeSkullItem(texture, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.settings")), false, false));
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

    public UUID getIslandOwner() {
        return ownerUUID;
    }

    public Player getViewer() {
        return viewer;
    }

    public void onClose(Player player) {
        IslandVault.getVaultSyncManager().unregisterViewer(island.getUniqueId(), player);
    }

    @Override
    public void handleMenuClose() {
        onClose(player);
    }
}
