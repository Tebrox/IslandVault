package de.tebrox.islandVault.Utils;

import me.kodysimpson.simpapi.colors.ColorTranslator;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.stream.Collectors;

public class GuiItemUtils {

    public static ItemStack FILLER_GLASS = makeItem(Material.BLACK_STAINED_GLASS_PANE, "", true, false);
    public static ItemStack NO_ITEMS = makeItem(Material.BARRIER, "You have no items", false, false);

    /**
     * @param material    The material to base the ItemStack on
     * @param displayName The display name of the ItemStack
     * @param hideTooltip Hide the Tooltip of the ItemStack
     * @param isEnchanted Enchant the item
     * @param lore        The lore of the ItemStack, with the Strings being automatically color coded with ColorTranslator
     * @return The constructed ItemStack object
     */
    public static ItemStack makeItem(Material material, String displayName, boolean hideTooltip, boolean isEnchanted, String... lore) {

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

}
