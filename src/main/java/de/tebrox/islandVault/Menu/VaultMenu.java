package de.tebrox.islandVault.Menu;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.VaultManager;
import de.tebrox.islandVault.Utils.IslandUtils;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import me.kodysimpson.simpapi.colors.ColorTranslator;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.PaginatedMenu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;
import java.util.stream.Collectors;

public class VaultMenu extends PaginatedMenu {
    protected ItemStack FILLER_GLASS = makeItem(Material.BLACK_STAINED_GLASS_PANE, "", true, false);
    protected ItemStack NO_ITEMS = makeItem(Material.BARRIER, "You have no items", false, false);
    private UUID adminOpenOwnerUUID = null;
    private UUID ownerUUID = null;

    public VaultMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
        maxItemsPerPage = 45;

        if(playerMenuUtility.getData("adminOpen")  != null) {
            adminOpenOwnerUUID = UUID.fromString(String.valueOf(playerMenuUtility.getData("adminOpen")));
        }

    }

    @Override
    public List<ItemStack> dataToItems() {
        String searchQuery = "";

        if(playerMenuUtility.getData("searchQuery") != null) {
            searchQuery = String.valueOf(playerMenuUtility.getData("searchQuery"));
        }

        ownerUUID = IslandUtils.getIslandOwnerUUID(playerMenuUtility.getOwner());;
        if(adminOpenOwnerUUID != null) {
            ownerUUID = adminOpenOwnerUUID;
        }


        if(ownerUUID == null) {
            List<ItemStack> temp = new ArrayList<>();
            temp.add(NO_ITEMS);
            for(int i = 1; i < maxItemsPerPage; i++) {
                temp.add(FILLER_GLASS);
            }
            return temp;
        }

        List<ItemStack> vaultItems = IslandVault.getVaultManager().getPlayerVaultItems(playerMenuUtility.getOwner(), ownerUUID);
        List<ItemStack> filteredItems = VaultManager.filterItems(player, vaultItems, searchQuery);
        filteredItems.sort(Comparator.comparing(ItemStack::getType));

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
        if(adminOpenOwnerUUID != null) {
            return IslandVault.getLanguageManager().translate(player, "menu.titleAdmin", Map.of("owner", Bukkit.getOfflinePlayer(adminOpenOwnerUUID).getName()));
        }
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
                        if(IslandVault.getVaultManager().addItemToVault(item.getType(), item.getAmount(), adminOpenOwnerUUID != null ? adminOpenOwnerUUID : island.getOwner(), player)) {
                            invalidateCache();
                            reloadItems();
                            player.getInventory().setItem(slot, null);
                        }
                    }else if(inventoryClickEvent.isRightClick()) {
                        if(IslandVault.getVaultManager().addItemToVault(item.getType(), 1, adminOpenOwnerUUID != null ? adminOpenOwnerUUID : island.getOwner(), player)) {
                            invalidateCache();
                            reloadItems();
                            ItemStack tempItem = new ItemStack(item.getType(), item.getAmount() - 1);
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
                        case 47:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                firstPage();
                            }
                            break;
                        case 48:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                prevPage();
                            }
                            break;
                        case 49:
                            player.closeInventory();
                            break;
                        case 50:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                nextPage();
                            }
                            break;
                        case 51:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                lastPage();
                            }
                            break;
                        case 53:
                            if(!inventoryClickEvent.getCurrentItem().isSimilar(FILLER_GLASS)) {
                                if(inventoryClickEvent.isLeftClick()) {
                                    IslandVault.getVaultManager().getVaults().get(player.getUniqueId()).setAutoCollect(!IslandVault.getVaultManager().getVaults().get(player.getUniqueId()).getAutoCollect());
                                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                                    reloadItems();
                                }

                                else if(inventoryClickEvent.isRightClick()) {
                                    player.closeInventory();
                                    player.playSound(player, Sound.ENTITY_EXPERIENCE_BOTTLE_THROW, 1, 1);
                                    IslandUtils.showRadiusParticles(player, player.getLocation(), 5);
                                }

                            }
                            break;

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
                        ItemStack tempItem = IslandVault.getVaultManager().getItemFromVault(cursor.getType(), 1, adminOpenOwnerUUID != null ? adminOpenOwnerUUID : island.getOwner(), player);
                        if(tempItem != null) {
                            ItemStack handItem = new ItemStack(cursor.getType(), cursorAmount + tempItem.getAmount());
                            player.setItemOnCursor(handItem);

                            invalidateCache();
                            reloadItems();
                        }
                    } else if (cursor == null || cursor.getType() == Material.AIR && item != null) { //taking item out of dsu

                        Material mat = item.getType();
                        ItemStack handItem;
                        int amount = 0;
                        if(isLeftClick) {
                            amount = item.getMaxStackSize();
                        }else if (isRightClick) {
                            amount = 1;
                        }
                        handItem = IslandVault.getVaultManager().getItemFromVault(mat, amount, adminOpenOwnerUUID != null ? adminOpenOwnerUUID : island.getOwner(), player);

                        if(handItem == null) return;

                        if(!isShiftClick) {
                            player.setItemOnCursor(handItem);
                        }else{
                            if(player.getInventory().firstEmpty() != -1){
                                player.getInventory().addItem(handItem);
                            }
                        }
                        invalidateCache();
                        reloadItems();

                    } else if (cursor != null && cursor.getType() != Material.AIR) {
                        Material material = cursor.getType();
                        int amount = cursor.getAmount();

                        if (IslandVault.getVaultManager().addItemToVault(material, amount, adminOpenOwnerUUID != null ? adminOpenOwnerUUID : island.getOwner(), player)) {
                            invalidateCache();
                            reloadItems();
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

    @Override
    protected void addMenuBorder() {

        if(getCurrentPage() > 1) {
            // First page button
            inventory.setItem(47, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.firstPage")), false, true));
            // Previous page button
            inventory.setItem(48, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.previousPage")), false, false));
        }else{
            inventory.setItem(47, FILLER_GLASS);
            inventory.setItem(48, FILLER_GLASS);
        }

        if(getCurrentPage() < getTotalPages()) {
            // Next page button
            inventory.setItem(50, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.nextPage")), false, false));
            // Last page button
            inventory.setItem(51, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.lastPage")), false, true));
        }else{
            inventory.setItem(50, FILLER_GLASS);
            inventory.setItem(51, FILLER_GLASS);
        }

        // Close button
        inventory.setItem(49, makeItem(Material.BARRIER, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.close")), false, false));

        int maxRadius = LuckPermsUtils.getMaxRadiusFromPermissions(IslandUtils.getIslandOwnerUUID(playerMenuUtility.getOwner()));
        if(maxRadius > 0) {
            String displayName = ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.item.radius.displayName"));
            Material material = Material.HOPPER;
            boolean isEnabled = IslandVault.getVaultManager().getVaults().get(ownerUUID).getAutoCollect();

            String stateParam = isEnabled ? "state.active": "state.inactive";
            String state = IslandVault.getLanguageManager().translate(player, stateParam);

            String bStateParam = isEnabled ? "state.disable": "state.enable";
            String bState = IslandVault.getLanguageManager().translate(player, bStateParam);

            List<String> lore = IslandVault.getLanguageManager().translateList(player, "menu.item.radius.itemLore", Map.of("radius", String.valueOf(maxRadius), "state", state, "booleanState", bState));


            inventory.setItem(53, makeItem(material, displayName, false, isEnabled, lore.toArray(new String[0])));
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
}
