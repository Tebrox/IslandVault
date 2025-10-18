package de.tebrox.islandVault.Menu;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.VaultManager;
import de.tebrox.islandVault.Utils.*;
import me.kodysimpson.simpapi.colors.ColorTranslator;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.PaginatedMenu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;
import java.util.stream.Collectors;

public class VaultMenu extends PaginatedMenu {

    private String islandId;
    private final Player viewer;

    private ItemSortUtil.SortOption currentOption;
    private ItemSortUtil.SortDirection currentDirection;
    private final List<ItemSortUtil.SortOption> options = Arrays.asList(ItemSortUtil.SortOption.values());

    public VaultMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
        this.viewer = playerMenuUtility.getOwner();
        this.maxItemsPerPage = 45;

        // Prüfe, ob Admin-Menü geöffnet wurde
        if (playerMenuUtility.getData("adminOpen") != null) {
            this.islandId = playerMenuUtility.getData("adminOpen").toString();
        } else {
            Island island = IslandUtils.getIslandManager().getIslandAt(viewer.getLocation()).orElse(null);
            if (island != null) {
                this.islandId = island.getUniqueId();
            }
        }

        if (this.islandId != null)
            IslandVault.getVaultManager().registerViewer(viewer, islandId, this);
    }

    @Override
    public List<ItemStack> dataToItems() {
        String searchQuery = Optional.ofNullable(playerMenuUtility.getData("searchQuery"))
                .map(Object::toString)
                .orElse("");

        if (islandId == null) {
            return Collections.singletonList(GuiItemUtils.NO_ITEMS);
        }

        currentOption = PlayerDataUtils.loadSortOption(viewer);
        currentDirection = PlayerDataUtils.loadSortDirection(viewer);

        // Sichtbare Items für diesen Spieler
        List<ItemStack> visibleItems = IslandVault.getVaultManager().getVisibleItemsForPlayer(viewer, islandId);

        // Filter & Sortierung anwenden
        List<ItemStack> filteredItems = VaultManager.filterItems(viewer, visibleItems, searchQuery);
        ItemSortUtil.sortItems(filteredItems, islandId, viewer, currentOption, currentDirection);

        if (filteredItems.isEmpty()) {
            List<ItemStack> empty = new ArrayList<>();
            empty.add(GuiItemUtils.NO_ITEMS);
            while (empty.size() < maxItemsPerPage) empty.add(GuiItemUtils.FILLER_GLASS);
            return empty;
        }

        int rest = filteredItems.size() % maxItemsPerPage;
        if (rest > 0) {
            int z = maxItemsPerPage - rest;
            for (int i = 0; i < z; i++) filteredItems.add(GuiItemUtils.FILLER_GLASS);
        }
        return filteredItems;
    }

    @Override
    public @Nullable HashMap<Integer, ItemStack> getCustomMenuBorderItems() {
        return null;
    }

    @Override
    public String getMenuName() {
        return IslandVault.getLanguageManager().translate(viewer, "menu.title");
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
    public void handleMenu(InventoryClickEvent event) throws MenuManagerNotSetupException, MenuManagerException {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (islandId == null) return;

        VaultManager vaultManager = IslandVault.getVaultManager();

        // Klick im eigenen Inventar → Item hinzufügen
        if (event.getClickedInventory() == player.getInventory()) {
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            if (event.isShiftClick()) {
                event.setCancelled(true);
                int amount = event.isLeftClick() ? item.getAmount() : 1;
                String key = IslandVault.getItemManager().buildPermissionKey(item);
                if (vaultManager.addItemToIslandVault(key, amount, islandId, player)) {
                    refreshData();
                    item.setAmount(item.getAmount() - amount);
                    if (item.getAmount() <= 0) player.getInventory().setItem(event.getSlot(), null);
                }
            }
            return;
        }

        // Klick im Vault selbst
        if (event.getClickedInventory() != player.getInventory()) {
            int slot = event.getSlot();

            // Bedienelemente (untere Leiste)
            if (slot >= 45) {
                handleControlButtons(event, player);
                return;
            }

            // Item-Entnahme
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            event.setCancelled(true);
            String key = IslandVault.getItemManager().buildPermissionKey(clicked);

            if (event.isShiftClick() || event.isLeftClick() || event.isRightClick()) {
                int amount = event.isLeftClick() ? clicked.getMaxStackSize() : 1;
                ItemStack taken = vaultManager.takeItemFromIslandVault(key, amount, islandId, player);

                if (taken == null) return;

                if (event.isShiftClick()) {
                    Map<Integer, ItemStack> leftovers = player.getInventory().addItem(taken);
                    leftovers.values().forEach(left ->
                            player.getWorld().dropItem(player.getLocation().add(0, 1, 0), left));
                } else {
                    player.setItemOnCursor(taken);
                }

                refreshData();
            }
        }
    }

    private void handleControlButtons(InventoryClickEvent e, Player player) {
        int slot = e.getSlot();
        e.setCancelled(true);

        switch (slot) {
            case 45 -> { // Sortierung
                if (e.isLeftClick()) {
                    int nextIndex = (options.indexOf(currentOption) + 1) % options.size();
                    currentOption = options.get(nextIndex);
                } else if (e.isRightClick()) {
                    currentDirection = (currentDirection == ItemSortUtil.SortDirection.ASCENDING)
                            ? ItemSortUtil.SortDirection.DESCENDING
                            : ItemSortUtil.SortDirection.ASCENDING;
                }
                PlayerDataUtils.saveSortSettings(player, currentOption, currentDirection);
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                refreshData();
            }
            case 47 -> firstPage();
            case 48 -> prevPage();
            case 49 -> player.closeInventory();
            case 50 -> nextPage();
            case 51 -> lastPage();
            case 52 -> { // Backpack öffnen
                player.openInventory(IslandVault.getVaultManager().getIslandVaults().get(islandId).getBackpack());
                player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1, 1);
            }
            case 53 -> { // Autocollect toggeln / Radius zeigen
                var data = IslandVault.getVaultManager().getIslandVaults().get(islandId);
                data.setAutoCollect(!data.isAutoCollectEnabled());
                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                reloadItems();
            }
        }
    }

    @Override
    public void addMenuBorder() {
        inventory.setItem(45, makeSortItem(Material.COMPASS));

        inventory.setItem(47, page > 0
                ? GuiItemUtils.makeItem(Material.BOOK, "§eErste Seite", false, true)
                : GuiItemUtils.FILLER_GLASS);

        inventory.setItem(48, page > 0
                ? GuiItemUtils.makeItem(Material.BOOK, "§eVorherige Seite", false, false)
                : GuiItemUtils.FILLER_GLASS);

        inventory.setItem(49, GuiItemUtils.makeItem(Material.BARRIER, "§cSchließen", false, false));

        inventory.setItem(50, GuiItemUtils.makeItem(Material.BOOK, "§eNächste Seite", false, false));
        inventory.setItem(51, GuiItemUtils.makeItem(Material.BOOK, "§eLetzte Seite", false, true));

        inventory.setItem(52, GuiItemUtils.makeItem(Material.CHEST, "§5Inselrucksack", false, false));

        var data = IslandVault.getVaultManager().getIslandVaults().get(islandId);
        boolean active = data.isAutoCollectEnabled();

        inventory.setItem(53, GuiItemUtils.makeItem(Material.HOPPER,
                active ? "§aAutocollect aktiv" : "§cAutocollect inaktiv",
                false, active));

        // Leere Slots auffüllen
        for (int i = 45; i < 54; i++) {
            if (inventory.getItem(i) == null)
                inventory.setItem(i, GuiItemUtils.FILLER_GLASS);
        }
    }

    private ItemStack makeSortItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String label = switch (currentOption) {
            case ID -> IslandVault.getLanguageManager().translate(viewer, "menu.item.sort.options.id");
            case AMOUNT -> IslandVault.getLanguageManager().translate(viewer, "menu.item.sort.options.amount");
            case LOCALIZED_NAME -> IslandVault.getLanguageManager().translate(viewer, "menu.item.sort.options.name");
        };
        String arrow = currentDirection == ItemSortUtil.SortDirection.ASCENDING ? "⬆" : "⬇";

        meta.setDisplayName(ChatColor.YELLOW + label + " " + arrow);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        List<ItemStack> items = getItems();
        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= items.size()) break;
            inventory.setItem(i, items.get(index));
        }
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());
        setMenuItems();
        viewer.openInventory(inventory);
        playerMenuUtility.pushMenu(this);
    }

    public String getIslandId() {
        return islandId;
    }

    public Player getViewer() {
        return viewer;
    }

    public void onClose(Player player) {
        IslandVault.getVaultManager().unregisterViewer(player, islandId);
    }

    @Override
    public void handleMenuClose() {
        onClose(viewer);
    }
}
