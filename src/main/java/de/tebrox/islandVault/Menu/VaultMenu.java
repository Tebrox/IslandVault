package de.tebrox.islandVault.Menu;

import de.tebrox.islandVault.IslandItemList;
import de.tebrox.islandVault.IslandSession;
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

    private IslandSession islandSession;
    private IslandItemList islandItemList;
    private Island island;
    private VaultData vaultData;

    private String searchQuery = "";
    private List<ItemStack> currentPageItems;
    private int currentPageItemCount;

    private final Player viewer;


    public VaultMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
        this.viewer = playerMenuUtility.getOwner();
        this.maxItemsPerPage = 45;
        this.islandSession = IslandVault.getSessionManager().getSessionByPlayer(viewer.getUniqueId()).orElse(null);

        this.island = islandSession.getIsland();

        IslandVault.getVaultSyncManager().registerViewer(island.getUniqueId(), viewer, this);

        this.vaultData = islandSession.getVaultData();
        this.ownerUUID = vaultData.getOwnerUUID();

        this.islandItemList = islandSession.getItemList();

        Object queryObj = playerMenuUtility.getData("searchQuery");
        if (queryObj != null) {
            this.searchQuery = String.valueOf(queryObj);
        }

    }

//    @Override
//    public List<ItemStack> dataToItems() {
//        String searchQuery = "";
//
//        if(playerMenuUtility.getData("searchQuery") != null) {
//            searchQuery = String.valueOf(playerMenuUtility.getData("searchQuery"));
//        }
//
//
//        if(ownerUUID == null) {
//            List<ItemStack> temp = new ArrayList<>();
//            temp.add(NO_ITEMS);
//            for(int i = 1; i < maxItemsPerPage; i++) {
//                temp.add(FILLER_GLASS);
//            }
//            return temp;
//        }
//
//        currentOption = PlayerDataUtils.loadSortOption(playerMenuUtility.getOwner());
//        currentDirection = PlayerDataUtils.loadSortDirection(playerMenuUtility.getOwner());
//        List<ItemStack> filteredItems = vaultData.filterItems(playerMenuUtility.getOwner(), searchQuery);
//
//        ItemSortUtil.sortItems(filteredItems, vaultData, player, currentOption, currentDirection);
//
//        Player player = playerMenuUtility.getOwner();
//        boolean isOp = player.isOp();
//
//        for (int i = 0; i < filteredItems.size(); i++) {
//            ItemStack item = filteredItems.get(i);
//            if (item == null || item.getType().isAir()) continue;
//
//            ItemMeta meta = item.getItemMeta();
//            if (meta == null) continue;
//
//            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
//
//            lore = IslandVault.getLanguageManager().translateList(player, "menu.item.vaultItem.itemLore", Map.of("amount", String.valueOf(vaultData.getAmount(item)), "maxStackSize", String.valueOf(item.getMaxStackSize())), true);
//
//            // Zusätzliche OP-Lore, falls OP
//            if (isOp && IslandVault.getPlugin().isDebug()) {
//                lore.add("§8§m------------------------");
//                lore.add("§6§lDEBUG-INFO");
//                lore.add("§7Material: §f" + item.getType());
//                // weitere Debug-Infos ...
//                lore.add("§8§m------------------------");
//            }
//
//            meta.setLore(lore);
//            item.setItemMeta(meta);
//
//            filteredItems.set(i, item);
//        }
//
//        int size = filteredItems.size();
//        int rest = size % maxItemsPerPage;
//        if(rest > 0) {
//            int z = maxItemsPerPage - rest;
//            for(int i = 0; i < z; i++) {
//                filteredItems.add(FILLER_GLASS);
//            }
//        }
//        return filteredItems;
//    }

    @Override
    public List<ItemStack> dataToItems() {
        List<ItemStack> allItems = dataToItemsFullList();

        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, allItems.size());

        if (startIndex >= allItems.size()) {
            return Collections.emptyList();
        }

        return allItems.subList(startIndex, endIndex);
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
    public void handleMenu(InventoryClickEvent event) throws MenuManagerNotSetupException, MenuManagerException {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        //Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);
        if (island == null) return;

        // Klick im Spieler-Inventar (zum Einlagern mit Shift-Klick)
        if (event.getClickedInventory() == player.getInventory()) {
            ItemStack clickedItem = event.getCurrentItem();
            int slot = event.getSlot();

            if (clickedItem == null || clickedItem.getType().isAir()) return;

            if (event.isShiftClick()) {
                event.setCancelled(true);
                if (event.isLeftClick() || event.isRightClick()) {
                    int amountToAdd = clickedItem.getAmount();
                    if (vaultData.add(clickedItem, amountToAdd)) {
                        refreshData();
                        if (event.isLeftClick()) {
                            player.getInventory().setItem(slot, null);
                        } else if (event.isRightClick()) {
                            ItemStack temp = clickedItem.clone();
                            temp.setAmount(temp.getAmount() - 1);
                            if (temp.getAmount() <= 0) {
                                player.getInventory().setItem(slot, null);
                            } else {
                                player.getInventory().setItem(slot, temp);
                            }
                        }
                    }
                }
            }
            return;
        }

        // Klick im Vault-Menü Inventar
        if (event.getClickedInventory() != player.getInventory()) {
            int slot = event.getSlot();
            ItemStack cursor = event.getCursor();
            ItemStack currentItem = event.getCurrentItem();

            boolean isLeftClick = event.isLeftClick();
            boolean isRightClick = event.isRightClick();
            boolean isShiftClick = event.isShiftClick();

            // Menü-Buttons in den Slots 45-54 abfangen
            if (slot >= 45 && slot <= 54) {
                event.setCancelled(true);

                if (currentItem == null || currentItem.isSimilar(FILLER_GLASS)) return;

                switch (slot) {
                    case 45:
                        if (isLeftClick) {
                            int nextIndex = (options.indexOf(currentOption) + 1) % options.size();
                            currentOption = options.get(nextIndex);
                        } else if (isRightClick) {
                            currentDirection = (currentDirection == ItemSortUtil.SortDirection.ASCENDING)
                                    ? ItemSortUtil.SortDirection.DESCENDING
                                    : ItemSortUtil.SortDirection.ASCENDING;
                        }
                        PlayerDataUtils.saveSortSettings(player, currentOption, currentDirection);
                        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                        refreshData();
                        break;

                    case 47:
                        firstPage();
                        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                        break;

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

                    case 51:
                        lastPage();
                        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                        break;

                    case 53:
                        MenuManager.getPlayerMenuUtility(player).setData("previousMenu", VaultMenu.class);
                        MenuManager.getPlayerMenuUtility(player).setData("vaultData", vaultData);
                        MenuManager.openMenu(SettingsMenu.class, player);
                        SoundUtils.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                        break;

                    default:
                        // Kein weiterer Fall nötig
                }
                return;
            }

            // Slots mit Items im Vault (0-44)
            event.setCancelled(true);

            // Defensive Null-Checks
            if (currentItem == null || currentItem.getType().isAir()) return;

            // Fall 1: Rechtsklick mit einem Item auf Cursor gleicher Typ (kein Shift) => 1 Item hinzufügen
            if (cursor != null && !cursor.getType().isAir()
                    && cursor.getType() == currentItem.getType()
                    && !isShiftClick && isRightClick) {

                if(cursor.getAmount() + 1 > cursor.getMaxStackSize()) return;

                ItemStack tempExtracted = vaultData.extractItem(ItemStackKey.of(cursor), 1);
                if (tempExtracted != null) {
                    ItemStack newCursor = new ItemStack(cursor.getType(), cursor.getAmount() + tempExtracted.getAmount());
                    player.setItemOnCursor(newCursor);
                    refreshData();
                }
                return;
            }

            // Fall 2: Cursor leer, Item anklicken => Item entnehmen
            if ((cursor == null || cursor.getType().isAir()) && currentItem != null) {
                int amountToExtract = isLeftClick ? currentItem.getMaxStackSize() : 1;

                ItemStack extracted = vaultData.extractItem(ItemStackKey.of(currentItem), amountToExtract);
                if (extracted == null) return;

                if (!isShiftClick) {
                    player.setItemOnCursor(extracted);
                } else {
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(extracted);
                    } else {
                        // Kein Platz im Inventar, Item in Cursor setzen als Fallback
                        player.setItemOnCursor(extracted);
                    }
                }
                refreshData();
                return;
            }

            // Fall 3: Cursor hält Item (nicht Luft), Item anklicken => Item zurücklegen
            if (cursor != null && !cursor.getType().isAir() && isLeftClick) {
                int amountToAdd = cursor.getAmount();

                if (vaultData.add(cursor, amountToAdd)) {
                    refreshData();
                    player.setItemOnCursor(null);
                }
                return;
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

        inventory.setItem(47, FILLER_GLASS);
        if(getCurrentPage() > 1) {
            // Previous page button
            texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTE4NWM5N2RiYjgzNTNkZTY1MjY5OGQyNGI2NDMyN2I3OTNhM2YzMmE5OGJlNjdiNzE5ZmJlZGFiMzVlIn19fQ==";
            inventory.setItem(48, makeSkullItem(texture, IslandVault.getLanguageManager().translate(player, "menu.previousPage", Map.of("currentPage", String.valueOf(getCurrentPage()), "previousPage", String.valueOf(getCurrentPage() - 1), "nextPage", String.valueOf(getCurrentPage() + 1), "maxPage", String.valueOf(getTotalPages()))), false, false));
            //inventory.setItem(48, makeItem(Material.BOOK, ColorTranslator.translateColorCodes(IslandVault.getLanguageManager().translate(player, "menu.previousPage")), false, false));
        }else{
            inventory.setItem(48, FILLER_GLASS);
        }

        if(getCurrentPage() < getTotalPages()) {
            texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzFjMGVkZWRkNzExNWZjMWIyM2Q1MWNlOTY2MzU4YjI3MTk1ZGFmMjZlYmI2ZTQ1YTY2YzM0YzY5YzM0MDkxIn19fQ==";
            // Next page button
            inventory.setItem(50, makeSkullItem(texture, IslandVault.getLanguageManager().translate(player, "menu.nextPage", Map.of("currentPage", String.valueOf(getCurrentPage()), "previousPage", String.valueOf(getCurrentPage() - 1), "nextPage", String.valueOf(getCurrentPage() + 1), "maxPage", String.valueOf(getTotalPages()))), false, false));
        }else{
            inventory.setItem(50, FILLER_GLASS);

        }
        inventory.setItem(51, FILLER_GLASS);
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

    @Override
    public int getTotalPages() {
        int totalItems = getTotalItemsCount();
        if (totalItems == 0) return 1; // Mindestens 1 Seite anzeigen

        return ((totalItems - 1) / maxItemsPerPage) + 1;
    }

    @Override
    public void refreshData() {
        invalidateCache();
        reloadItems();
    }

    @Override
    public boolean nextPage() {
        int totalPages = getTotalPages();
        if (page < totalPages - 1) {
            page++;
            refreshData();  // Cache invalidieren & Items neu laden
            return true;
        }
        return false;
    }

    @Override
    public boolean prevPage() {
        if (page == 0) {
            return false;  // Bereits auf der ersten Seite
        }
        page--;
        refreshData();  // Cache leeren und Items neu laden
        return true;
    }

    public int getTotalItemsCount() {
        // Beispiel: Alle gefilterten Items holen (ohne Paging), nur die Anzahl zurückgeben
        return  islandItemList.getAllowedItemStacks().size();
    }

    public List<ItemStack> dataToItemsFullList() {
        Player player = playerMenuUtility.getOwner();
        UUID ownerUUID = vaultData.getOwnerUUID();

        // 1. Hole alle erlaubten Items der Insel (IslandItemList)
        IslandItemList islandItemList = IslandVault.getSessionManager().getSessionByPlayer(ownerUUID).orElse(null).getItemList();
        if (islandItemList == null) {
            return Collections.emptyList();
        }

        // 2. Hole alle Items aus dem Vault mit Mengenangaben
        Map<ItemStackKey, Integer> vaultItems = vaultData.getItems();

        // 3. Entscheide, ob nur Items mit Menge angezeigt werden sollen
        boolean showOnlyWithAmount = PlayerDataUtils.loadShowOnlyItemsWithAmount(player);

        Map<ItemStackKey, Integer> combinedItems;

        if (showOnlyWithAmount) {
            // Nur Items, die im Vault vorhanden sind UND auf der IslandItemList erlaubt sind
            combinedItems = vaultItems.entrySet().stream()
                    .filter(e -> islandItemList.isItemAllowed(e.getKey()) && e.getValue() > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            // Alle erlaubten Items aus IslandItemList, Menge aus Vault (0 falls nicht vorhanden)
            combinedItems = new HashMap<>();

            for (ItemStack allowedItem : islandItemList.getAllowedItemStacks()) {
                ItemStackKey key = ItemStackKey.of(allowedItem);
                int amount = vaultItems.getOrDefault(key, 0);
                combinedItems.put(key, amount);
            }
        }

        // 4. Filter nach Suchbegriff (falls vorhanden)
        String searchQuery;
        if (playerMenuUtility.getData("searchQuery") != null) {
            searchQuery = String.valueOf(playerMenuUtility.getData("searchQuery")).toLowerCase(Locale.ROOT);
        } else {
            searchQuery = "";
        }

        // 5. Map zu ItemStack + Filter auf Name/Lore mit Suchbegriff
        return combinedItems.keySet().stream()
                .map(ItemStackKey::toItemStack)
                .filter(item -> {
                    if (item == null || item.getType().isAir()) return false;

                    if (searchQuery.isEmpty()) return true;

                    String name = ItemNameTranslator.getLocalizedName(item, player).toLowerCase(Locale.ROOT);

                    if (name.contains(searchQuery)) return true;

                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        for (String line : meta.getLore()) {
                            if (line != null && line.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                                return true;
                            }
                        }
                    }

                    return false;
                })
                .collect(Collectors.toList());
    }
}