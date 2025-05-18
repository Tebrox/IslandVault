package de.tebrox.islandVault.menu;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.items.ItemManager;
import de.tebrox.islandVault.manager.VaultManager;
import me.kodysimpson.simpapi.colors.ColorTranslator;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.PaginatedMenu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class VaultMenu extends PaginatedMenu {
    protected ItemStack FILLER_GLASS = makeItem(Material.BLACK_STAINED_GLASS_PANE, "", true);

    public VaultMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    @Override
    public List<ItemStack> dataToItems() {
        return IslandVault.getVaultManager().getPlayerVaultItems(playerMenuUtility.getOwner().getUniqueId());
    }

    @Override
    public @Nullable HashMap<Integer, ItemStack> getCustomMenuBorderItems() {
        return null;
    }

    @Override
    public String getMenuName() {
        return IslandVault.getPlugin().getConfig().getString("messages.menuTitle");
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
    public void handleMenu(InventoryClickEvent inventoryClickEvent) throws MenuManagerNotSetupException, MenuManagerException {

    }

    /**
     * @param material    The material to base the ItemStack on
     * @param displayName The display name of the ItemStack
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
        //TODO menu items at correct page
        if(getCurrentPage() > 0) {
            // First page button
            inventory.setItem(47, makeItem(Material.DARK_OAK_BUTTON, ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString("messages.firstPage")), false));
            // Previous page button
            inventory.setItem(48, makeItem(Material.DARK_OAK_BUTTON, ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString("messages.previousPage")), false));
        }else{
            inventory.setItem(47, FILLER_GLASS);
            inventory.setItem(48, FILLER_GLASS);
        }

        if(page < getTotalPages()) {
            // Next page button
            inventory.setItem(50, makeItem(Material.DARK_OAK_BUTTON, ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString("messages.nextPage")), false));
            // Last page button
            inventory.setItem(51, makeItem(Material.DARK_OAK_BUTTON, ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString("messages.lastPage")), false));
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

    /**
     * Place each item in the paginated menu, automatically coded by default but override if you want custom functionality. Calls the loopCode() method you define for each item returned in the getData() method
     */
    @Override
    public void setMenuItems() {
        addMenuBorder();

        List<ItemStack> items = getItems(); // Use cached items

        int slot = 0;
        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= items.size()) break;

            if (slot % 9 == 8) slot += 2;

            inventory.setItem(slot, items.get(index));
            slot++;
        }
    }
}
