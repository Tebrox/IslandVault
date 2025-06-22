package de.tebrox.islandVault.Menu;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Utils.BentoBoxRanks;
import de.tebrox.islandVault.Utils.IslandUtils;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import de.tebrox.islandVault.Utils.PlayerDataUtils;
import de.tebrox.islandVault.VaultData;
import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.MenuManager;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * A settings menu for managing island vault preferences.
 *
 * This menu allows players to configure:
 * - Vault access level (based on BentoBox ranks)
 * - Auto-collection toggle
 * - Item display filtering
 * - Particle preview of collection radius
 *
 * It also allows returning to a previous menu, if one was specified.
 */
public class SettingsMenu extends Menu {

    private VaultData vaultData;
    private Class<?> previousMenu = null;

    private File bentoBoxfile;
    private YamlConfiguration bentoBoxconfig;

    /**
     * Constructs a new SettingsMenu using the given {@link PlayerMenuUtility}.
     *
     * This pulls data from the utility object:
     * - "vaultData" for vault information
     * - "previousMenu" to return to the calling menu (if any)
     *
     * It also loads the BentoBox locale config file for translating rank names.
     *
     * @param playerMenuUtility the player's menu utility context
     */
    public SettingsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);

        if(playerMenuUtility.getData("vaultData") != null) {
            vaultData = (VaultData) playerMenuUtility.getData("vaultData");
        }

        if(playerMenuUtility.getData("previousMenu") != null) {
            previousMenu = (Class<?>) playerMenuUtility.getData("previousMenu");
        }

        String playerLang = IslandVault.getLanguageManager().getPlayerLanguageKey(player);
        if(playerLang.equals("en")) {
            playerLang = "en_US";
        }

        bentoBoxfile = new File("plugins/BentoBox/locales/BentoBox/" + playerLang + ".yml");
        bentoBoxconfig = YamlConfiguration.loadConfiguration(bentoBoxfile);
    }

    @Override
    public String getMenuName() {
        return IslandVault.getLanguageManager().translate(player, "menu.settings");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public boolean cancelAllClicks() {
        return true;
    }

    /**
     * Handles clicks within the settings menu.
     * Supports left/right/shift click behaviors for various settings buttons.
     *
     * @param inventoryClickEvent the inventory click event
     */
    @Override
    public void handleMenu(InventoryClickEvent inventoryClickEvent) {
        if(inventoryClickEvent.getWhoClicked() instanceof Player) {
            ItemStack item = inventoryClickEvent.getCurrentItem();

            if(item == null) {
                return;
            }

            switch(item.getType()) {
                case Material.ARROW:
                    Class<?> previousMenuClass;
                    previousMenuClass = previousMenu;

                    if (previousMenuClass == null) {
                        playerMenuUtility.getOwner().sendMessage("§cKein vorheriges Menü gefunden.");
                        return;
                    }

                    if (!Menu.class.isAssignableFrom(previousMenuClass)) {
                        playerMenuUtility.getOwner().sendMessage("§cGespeicherte Klasse ist kein Menü.");
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    Class<? extends Menu> menuClass = (Class<? extends Menu>) previousMenuClass;

                    try {
                        MenuManager.openMenu(menuClass, playerMenuUtility.getOwner());
                    } catch (Exception e) {
                        playerMenuUtility.getOwner().sendMessage("§cFehler beim Öffnen des Menüs.");
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

                    break;
                case Material.HOPPER:
                    if(inventoryClickEvent.isLeftClick()) {
                        vaultData.setAutoCollect(!vaultData.getAutoCollect());
                        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    }
                    else if(inventoryClickEvent.isRightClick()) {
                        player.closeInventory();
                        player.playSound(player, Sound.ENTITY_EXPERIENCE_BOTTLE_THROW, 1, 1);
                        IslandVault.getParticleManager().showRadiusBorder(player, "radius", player.getLocation(), 10, Particle.DUST, Color.BLUE, null);
                        //IslandUtils.showRadiusParticles(player, player.getLocation(), 5);
                    }
                    break;
                case Material.COMPASS:
                    PlayerDataUtils.saveShowOnlyItemsWithAmount(player, !PlayerDataUtils.loadShowOnlyItemsWithAmount(player));

                    break;
                default:
                    break;
            }
            reloadItems();
        }
    }

    @Override
    public void setMenuItems() {
        List<ItemStack> buttons = createButtonList();

        for(int i = 0; i < buttons.size(); i++) {
            inventory.setItem(i, buttons.get(i));
        }
    }

    /**
     * Creates the list of interactive buttons for the menu based on current vault state and permissions.
     *
     * @return a list of menu buttons
     */
    private List<ItemStack> createButtonList() {
        List<ItemStack> list = new ArrayList<>();

        if(previousMenu != null) {
            list.add(makeItem(Material.ARROW, IslandVault.getLanguageManager().translate(player, "menu.back")));
        }

        if(vaultData.getOwnerUUID().equals(playerMenuUtility.getOwner().getUniqueId())) {
            list.add(makeItem(Material.HOPPER, IslandVault.getLanguageManager().translate(player, "menu.item.radius.displayName"), createAutocollectLore().toArray(new String[0])));
        }

        if(vaultData.getOwnerUUID().equals(playerMenuUtility.getOwner().getUniqueId())) {
            list.add(makeItem(Material.BOOK, IslandVault.getLanguageManager().translate(player, "menu.item.vaultAuthorization.displayName"), createAccessLore().toArray(new String[0])));
        }

        list.add(makeItem(Material.COMPASS, IslandVault.getLanguageManager().translate(player, "menu.item.vaultItemFilter.displayName"), createFilterLore().toArray(new String[0])));

        return list;
    }

    /**
     * Creates lore lines describing the current filter state.
     *
     * @return a list of lore lines
     */
    private List<String> createFilterLore() {
        String stateParam = "menu.item.vaultItemFilter." + (PlayerDataUtils.loadShowOnlyItemsWithAmount(player) ? "availableOnly" : "showAll");
        return IslandVault.getLanguageManager().translateList(player, stateParam);
    }

    /**
     * Creates lore for the auto-collect toggle item.
     *
     * @return list of translated lore strings
     */
    private List<String> createAutocollectLore() {
        String stateParam = "state." + (vaultData.getAutoCollect() ? "active" : "inactive");
        String state = IslandVault.getLanguageManager().translate(player, stateParam);

        stateParam = "state." + (vaultData.getAutoCollect() ? "disable" : "enable");
        String state2 = IslandVault.getLanguageManager().translate(player, stateParam);
        return IslandVault.getLanguageManager().translateList(player, "menu.item.radius.itemLore", Map.of("radius", String.valueOf(LuckPermsUtils.getMaxRadiusFromPermissions(vaultData.getOwnerUUID())),"state", state, "booleanState", state2), true);
    }

    /**
     * Creates lore for the access level toggle item.
     *
     * @return list of translated lore strings
     */
    private List<String> createAccessLore() {
        List<String> lore = new ArrayList<>();
        int currentLevel = vaultData.getAccessLevel();

        List<Integer> ACCESS_LEVELS = BentoBoxRanks.getSortedRoleIds();

        for (int level : ACCESS_LEVELS) {
            String name = BentoBoxRanks.getName(level);
            String translated = bentoBoxconfig.getString("ranks." + name.replace(" ", "-"), name);
            if (level >= currentLevel) {

                lore.add("§a✔ " + translated);
            } else {
                lore.add("§c✘ " + translated);
            }
        }

        lore.addAll(IslandVault.getLanguageManager().translateList(player, "menu.item.vaultAuthorization.itemLore", Map.of("ownerlabel", bentoBoxconfig.getString("ranks.owner", "Owner")), true));

        return lore;
    }

    /**
     * Returns the next access level in the sorted rank list, in forward or reverse direction.
     *
     * @param currentLevel current BentoBox access level
     * @param accessLevels sorted list of BentoBox rank IDs
     * @param forward true to cycle forward, false to cycle backward
     * @return the next rank ID
     */
    public static int getNextAccessLevel(int currentLevel, List<Integer> accessLevels, boolean forward) {
        if (accessLevels.isEmpty()) {
            throw new IllegalArgumentException("Liste der Levels ist leer.");
        }

        // Erstelle eine kopierte und umgedrehte Liste
        List<Integer> reversed = new ArrayList<>(accessLevels);
        Collections.reverse(reversed);

        int index = reversed.indexOf(currentLevel);
        if (index == -1) {
            return reversed.getFirst(); // Fallback
        }

        int size = reversed.size();
        int nextIndex;

        if (forward) {
            nextIndex = (index + 1) % size;
        } else {
            nextIndex = (index - 1 + size) % size;
        }

        return reversed.get(nextIndex);
    }
}
