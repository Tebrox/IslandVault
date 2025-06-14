package de.tebrox.islandVault.Menu;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Utils.BentoBoxRanks;
import de.tebrox.islandVault.Utils.IslandUtils;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.MenuManager;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SettingsMenu extends Menu {

    public SettingsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    @Override
    public String getMenuName() {
        return "Einstellungen";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public boolean cancelAllClicks() {
        return true;
    }

    @Override
    public void handleMenu(InventoryClickEvent inventoryClickEvent) throws MenuManagerNotSetupException, MenuManagerException {
        if(inventoryClickEvent.getWhoClicked() instanceof Player player) {
            ItemStack item = inventoryClickEvent.getCurrentItem();

            if(item == null) {
                return;
            }

            switch(item.getType()) {
                case Material.ARROW:
                    if(playerMenuUtility.getData("previousMenu") != null) {
                        Class<? extends Menu> previous = (Class<? extends Menu>) playerMenuUtility.getData("previousMenu");
                        System.out.println(getClass());
                        System.out.println(previous);

                        if (previous != null) {
                            MenuManager.openMenu(previous, playerMenuUtility.getOwner());
                        } else {
                            playerMenuUtility.getOwner().sendMessage("§cKein vorheriges Menü gefunden.");
                        }
                    }
                    break;
                case Material.OAK_SIGN:

                    break;
                case Material.HOPPER:
                    if(inventoryClickEvent.isLeftClick()) {
                        //IslandVault.getVaultManager().getVaults().get(player.getUniqueId()).setAutoCollect(!IslandVault.getVaultManager().getVaults().get(player.getUniqueId()).getAutoCollect());
                        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                        reloadItems();
                    }
                    else if(inventoryClickEvent.isRightClick()) {
                        player.closeInventory();
                        player.playSound(player, Sound.ENTITY_EXPERIENCE_BOTTLE_THROW, 1, 1);
                        IslandUtils.showRadiusParticles(player, player.getLocation(), 5);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void setMenuItems() {
        List<ItemStack> buttons = createButtonList();
    }

    private List<ItemStack> createButtonList() {
        List<ItemStack> list = new ArrayList<>();

        if(playerMenuUtility.getData("previousMenu") != null) {
            list.add(makeItem(Material.ARROW, "§cZurück"));
        }

        if(playerMenuUtility.getData("ownerUUID") != null && playerMenuUtility.getData("ownerUUID") == player.getUniqueId()) {
            list.add(makeItem(Material.HOPPER, "§aAuto-Collect Ein/Aus"));
        }

        if(playerMenuUtility.getData("ownerUUID") != null && playerMenuUtility.getData("ownerUUID") == player.getUniqueId()) {
            list.add(makeItem(Material.OAK_SIGN, "§aLagerberechtigung"));
        }

        return list;
    }

    private List<String> createAccessLore() {
        List<String> lore = new ArrayList<>();
        //int currentLevel = IslandVault.getVaultManager().getAccessLevel(playerMenuUtility.getOwner().getUniqueId());
        int currentLevel = BentoBoxRanks.getId("owner");


        List<Integer> ACCESS_LEVELS = BentoBoxRanks.getSortedRoleIds();

        for (int i = 0; i < ACCESS_LEVELS.size(); i++) {
            int level = ACCESS_LEVELS.get(i);
            String name = BentoBoxRanks.getName(level);

            if (level <= currentLevel) {
                lore.add("§a✔ " + name);
            } else {
                lore.add("§c✘ " + name);
            }
        }

        lore.add("");
        lore.add("Klicke auf das Buch, um das Zugriffslevel zu ändern.");

        return lore;
    }
}
