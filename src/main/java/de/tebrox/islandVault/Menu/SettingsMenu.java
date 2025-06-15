package de.tebrox.islandVault.Menu;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Utils.BentoBoxRanks;
import de.tebrox.islandVault.Utils.IslandUtils;
import de.tebrox.islandVault.VaultData;
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
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;

public class SettingsMenu extends Menu {

    private VaultData vaultData;
    private Island island;

    public SettingsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
        island = BentoBox.getInstance().getIslands().getIslandAt(player.getLocation()).orElse(null);
        this.vaultData = IslandVault.getVaultManager().getCachedVault(island.getUniqueId());
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
                case Material.BOOK:
                    if(inventoryClickEvent.isShiftClick()) {
                        vaultData.setAccessLevel(BentoBoxRanks.getId("owner"));
                    }else{
                        if(inventoryClickEvent.isLeftClick()) {
                            vaultData.setAccessLevel(getNextAccessLevel(vaultData.getAccessLevel(), BentoBoxRanks.getSortedRoleIds(), true));
                        }else if(inventoryClickEvent.isRightClick()) {
                            vaultData.setAccessLevel(getNextAccessLevel(vaultData.getAccessLevel(), BentoBoxRanks.getSortedRoleIds(), false));
                        }
                    }
                    IslandVault.getVaultManager().saveVaultAsync(vaultData);
                    reloadItems();

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

        for(int i = 0; i < buttons.size(); i++) {
            inventory.setItem(i, buttons.get(i));
        }
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
            list.add(makeItem(Material.BOOK, "§aLagerberechtigung", createAccessLore().toArray(new String[0])));
        }

        list.add(makeItem(Material.COMPASS, "Zeige nur Items mit Menge"));

        return list;
    }

    private List<String> createAccessLore() {
        List<String> lore = new ArrayList<>();
        int currentLevel = vaultData.getAccessLevel();

        List<Integer> ACCESS_LEVELS = BentoBoxRanks.getSortedRoleIds();

        for (int level : ACCESS_LEVELS) {
            String name = BentoBoxRanks.getName(level);

            if (level <= currentLevel) {
                lore.add("§a✔ " + name);
            } else {
                lore.add("§c✘ " + name);
            }
        }

        lore.add("");
        lore.add("Klicke auf das Buch, um das Zugriffslevel zu ändern.");
        lore.add("Linksklick: Zum nächsten Eintrag");
        lore.add("Rechtsklick: Zum vorherigen Eintrag");
        lore.add("Shiftklick: Zum Standardeintrag (Owner)");

        return lore;
    }

    public static int getNextAccessLevel(int currentLevel, List<Integer> accessLevels, boolean forward) {
        if (accessLevels.isEmpty()) {
            throw new IllegalArgumentException("Liste der Levels ist leer.");
        }

        int index = accessLevels.indexOf(currentLevel);
        if (index == -1) {
            // Optional: Zum ersten Element zurückfallen
            return accessLevels.get(0);
        }

        int size = accessLevels.size();
        int nextIndex;

        if (forward) {
            nextIndex = (index + 1) % size;
        } else {
            nextIndex = (index - 1 + size) % size;
            // Das +size stellt sicher, dass der Index nie negativ wird
        }

        return accessLevels.get(nextIndex);
    }
}
