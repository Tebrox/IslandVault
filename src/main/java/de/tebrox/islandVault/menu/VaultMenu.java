package de.tebrox.islandVault.menu;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.enums.Messages;
import me.kodysimpson.simpapi.colors.ColorTranslator;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.PaginatedMenu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class VaultMenu extends PaginatedMenu {
    protected ItemStack FILLER_GLASS = makeItem(Material.BLACK_STAINED_GLASS_PANE, "", true);

    public VaultMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
        maxItemsPerPage = 45;
    }

    @Override
    public List<ItemStack> dataToItems() {
        List<ItemStack> vaultItems = IslandVault.getVaultManager().getPlayerVaultItems(playerMenuUtility.getOwner().getUniqueId());
        Collections.sort(vaultItems, Comparator.comparing(ItemStack::getType));

        int size = vaultItems.size();
        int rest = size % maxItemsPerPage;
        if(rest > 0) {
            int z = maxItemsPerPage - rest;
            for(int i = 0; i < z; i++) {
                vaultItems.add(FILLER_GLASS);
            }
        }

        return vaultItems;
    }

    @Override
    public @Nullable HashMap<Integer, ItemStack> getCustomMenuBorderItems() {
        return null;
    }

    @Override
    public String getMenuName() {
        return IslandVault.getPlugin().getConfig().getString(Messages.MENU_TITLE.getLabel());
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
            if(inventoryClickEvent.getClickedInventory() == player.getInventory()) {
                int slot = inventoryClickEvent.getSlot();
                ItemStack item = inventoryClickEvent.getCurrentItem();
                if(inventoryClickEvent.isShiftClick()) {
                    inventoryClickEvent.setCancelled(true);
                    if(inventoryClickEvent.isLeftClick()) {
                        if(IslandVault.getVaultManager().addItemToVault(item.getType(), item.getAmount(), player)) {
                            invalidateCache();
                            reloadItems();
                            player.getInventory().setItem(slot, null);
                        }
                    }else if(inventoryClickEvent.isRightClick()) {
                        if(IslandVault.getVaultManager().addItemToVault(item.getType(), 1, player)) {
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
                            if(inventoryClickEvent.getCurrentItem() != FILLER_GLASS) {
                                firstPage();
                            }
                            break;
                        case 48:
                            if(inventoryClickEvent.getCurrentItem() != FILLER_GLASS) {
                                prevPage();
                            }
                            break;
                        case 49:
                            player.closeInventory();
                            break;
                        case 50:
                            if(inventoryClickEvent.getCurrentItem() != FILLER_GLASS) {
                                nextPage();
                            }
                            break;
                        case 51:
                            if(inventoryClickEvent.getCurrentItem() != FILLER_GLASS) {
                                lastPage();
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
                        ItemStack tempItem = IslandVault.getVaultManager().getItemFromVault(cursor.getType(), 1, player);
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
                        handItem = IslandVault.getVaultManager().getItemFromVault(mat, amount, player);

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

                        if (IslandVault.getVaultManager().addItemToVault(material, amount, player)) {
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
     * @param lore        The lore of the ItemStack, with the Strings being automatically color coded with ColorTranslator
     * @return The constructed ItemStack object
     */
    public ItemStack makeItem(Material material, String displayName, boolean hideTooltip, String... lore) {

        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(displayName);
        itemMeta.setHideTooltip(hideTooltip);

        //Automatically translate color codes provided
        itemMeta.setLore(Arrays.stream(lore).map(ColorTranslator::translateColorCodes).collect(Collectors.toList()));
        item.setItemMeta(itemMeta);

        return item;
    }

    @Override
    protected void addMenuBorder() {
        if(getCurrentPage() > 1) {
            // First page button
            inventory.setItem(47, makeItem(Material.WRITTEN_BOOK, ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString("messages.firstPage")), false));
            // Previous page button
            inventory.setItem(48, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString("messages.previousPage")), false));
        }else{
            inventory.setItem(47, FILLER_GLASS);
            inventory.setItem(48, FILLER_GLASS);
        }

        if(getCurrentPage() < getTotalPages()) {
            // Next page button
            inventory.setItem(50, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString("messages.nextPage")), false));
            // Last page button
            inventory.setItem(51, makeItem(Material.WRITTEN_BOOK, ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString("messages.lastPage")), false));
        }else{
            inventory.setItem(50, FILLER_GLASS);
            inventory.setItem(51, FILLER_GLASS);
        }

        // Close button
        inventory.setItem(49, makeItem(Material.BARRIER, ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString("messages.close")), false));

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
}
