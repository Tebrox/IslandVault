package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.InventoryHolder.FilterInventoryHolder;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Items.VaultChest;
import de.tebrox.islandVault.Items.VaultChestItem;
import de.tebrox.islandVault.Manager.VaultChestManager;
import de.tebrox.islandVault.Utils.GuiItemUtils;
import de.tebrox.islandVault.Utils.IslandUtils;
import de.tebrox.islandVault.Utils.PlayerVaultUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;

/**
 * Listener für Platzierung, Interaktion und Verwaltung von VaultChests.
 * Vollständig angepasst auf den VaultChestManager.
 */
public class VaultChestListener implements Listener {

    private static final String GUI_MARKER = "Füllt sich automatisch ins Lager";

    private VaultChestManager manager() {
        return IslandVault.getVaultChestManager();
    }

    // ================================================================
    // ===================== Platzieren / Entfernen ===================
    // ================================================================
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (VaultChestItem.isCustomChest(item)) {
            if (!IslandUtils.hasAccessToCurrentIsland(player)) {
                event.setCancelled(true);
                player.sendMessage("§cDu kannst auf dieser Insel keine Inseltruhen platzieren.");
                return;
            }

            Bukkit.getScheduler().runTask(IslandVault.getPlugin(), () -> {
                if (!(block.getState() instanceof Chest chestBlock)) return;

                // Verhindere Doppelkiste
                InventoryHolder holder = chestBlock.getInventory().getHolder();
                if (holder instanceof DoubleChest dc) {
                    Chest left = (Chest) dc.getLeftSide();
                    Chest right = (Chest) dc.getRightSide();
                    for (Chest c : new Chest[]{left, right}) {
                        Block cb = c.getBlock();
                        if (cb.getBlockData() instanceof org.bukkit.block.data.type.Chest cd) {
                            cd.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
                            cb.setBlockData(cd, true);
                        }
                    }
                }

                Island island = IslandUtils.getIslandManager().getIslandAt(block.getLocation()).orElse(null);
                VaultChest chest = new VaultChest(player.getUniqueId(), block.getLocation(), island);
                chest.getChestInventory().setContents(chestBlock.getInventory().getContents());

                manager().registerChest(chest);
                manager().saveChest(chest);

                player.sendMessage("§aInseltruhe platziert.");
            });
        } else {
            // Normale Chest, aber Verbindung zu VaultChest verhindern
            Bukkit.getScheduler().runTask(IslandVault.getPlugin(), () -> {
                if (!(block.getState() instanceof Chest chestBlock)) return;
                if (chestBlock.getInventory().getHolder() instanceof DoubleChest dc) {
                    Chest left = (Chest) dc.getLeftSide();
                    Chest right = (Chest) dc.getRightSide();

                    boolean containsVault =
                            manager().getChestAt(left.getLocation()) != null ||
                                    manager().getChestAt(right.getLocation()) != null;

                    if (containsVault) {
                        for (Chest c : new Chest[]{left, right}) {
                            Block cb = c.getBlock();
                            if (cb.getBlockData() instanceof org.bukkit.block.data.type.Chest cd) {
                                cd.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
                                cb.setBlockData(cd, true);
                            }
                        }
                    }
                }
            });
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        VaultChest chest = manager().getChestAt(block.getLocation());
        if (chest == null) return;

        Player player = event.getPlayer();
        if (!IslandUtils.hasAccessToCurrentIsland(player)) {
            event.setCancelled(true);
            player.sendMessage("§cDu darfst diese Truhe nicht zerstören.");
            return;
        }

        event.setDropItems(false);

        for (ItemStack item : chest.getChestInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
        }

        if (!(player.getGameMode() == GameMode.CREATIVE)) {
            block.getWorld().dropItemNaturally(block.getLocation(), VaultChestItem.createCustomChest());
        }

        manager().stopAutoTask(chest);
        manager().deleteChest(chest);

        player.sendMessage("§cInseltruhe zerstört.");
    }

    // ================================================================
    // ===================== Filter-GUI Interaktionen =================
    // ================================================================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        VaultChest chest = manager().getChestAt(event.getClickedBlock().getLocation());
        if (chest == null) return;

        Player player = event.getPlayer();
        if (!IslandUtils.hasAccessToCurrentIsland(player)) {
            player.sendMessage("§cDu hast keine Berechtigung für die Inseltruhe!");
            return;
        }

        if (player.isSneaking() && event.getAction().isRightClick()) {
            event.setCancelled(true);

            ItemStack[] filterItems = chest.loadFilter();
            FilterInventoryHolder holder = new FilterInventoryHolder(chest, event.getClickedBlock());
            Inventory inv = Bukkit.createInventory(holder, 18, "Filter-Einstellungen");
            holder.setInventory(inv);
            inv.setContents(filterItems);

            for (int i = 9; i < 18; i++) inv.setItem(i, GuiItemUtils.FILLER_GLASS);
            inv.setItem(13, createIoButton(chest.isInputChest()));

            player.openInventory(inv);
            chest.setLastEditor(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        Inventory topInv = e.getView().getTopInventory();
        VaultChest vaultChest = manager().getVaultChestFromHolder(topInv.getHolder());

        if (vaultChest == null || !vaultChest.isInputChest()) return;

        // Nur Input-Chests: Items mit Lore versehen
        for (int i = 0; i < topInv.getSize(); i++) {
            ItemStack item = topInv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                applyGuiLore(item, player, vaultChest);
                topInv.setItem(i, item);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;

        Inventory topInv = e.getView().getTopInventory();
        VaultChest vaultChest = manager().getVaultChestFromHolder(topInv.getHolder());

        if (vaultChest == null || !vaultChest.isInputChest()) return;

        // Beim Schließen alle GUI-Lores zurücksetzen
        for (int i = 0; i < topInv.getSize(); i++) {
            ItemStack item = topInv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                restoreOriginalLore(item);
                topInv.setItem(i, item);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = e.getView().getTopInventory(); // Truhe/GUI
        int rawSlot = e.getRawSlot();

        // VaultChest anhand Block-Location holen (Chest/DoubleChest)
        VaultChest vaultChest = manager().getVaultChestFromHolder(topInv.getHolder());

        if (vaultChest == null || !vaultChest.isInputChest()) return;

        ItemStack clicked = e.getCurrentItem(); // Item im Slot (vor der Aktion)
        ItemStack cursor = e.getCursor();       // Item auf dem Cursor (vor der Aktion)

        switch (e.getAction()) {
            // --- Mit der Hand ablegen: wir ändern den CURSOR, denn der wird in den Slot gelegt
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case SWAP_WITH_CURSOR: {
                if (cursor.getType() != Material.AIR) {
                    if (rawSlot < topInv.getSize()) {
                        // Cursor-Item wird gleich in die Truhe gelegt → Lore anhängen
                        applyGuiLore(cursor, player, vaultChest);
                    } else {
                        // Cursor-Item wird ins Spielerinventar gelegt → Original wiederherstellen
                        restoreOriginalLore(cursor);
                    }
                }
                break;
            }

            // --- Aufnehmen: wir ändern den SLOT, denn dieses Item wandert auf den Cursor
            case PICKUP_ALL:
            case PICKUP_HALF:
            case PICKUP_ONE:
            case PICKUP_SOME: {
                if (clicked == null || clicked.getType() == Material.AIR) break;

                if (rawSlot < topInv.getSize()) {
                    // Quelle: Truhe -> Slot-Item vorab auf Original setzen (geht dann auf Cursor)
                    restoreOriginalLore(clicked);
                    e.setCurrentItem(clicked);
                }
                break;
            }

            // --- Shift-Klick: Item wandert ins andere Inventar
            case MOVE_TO_OTHER_INVENTORY: {
                if (clicked == null || clicked.getType() == Material.AIR) break;

                if (rawSlot < topInv.getSize()) {
                    // aus Truhe -> Spielerinventar
                    restoreOriginalLore(clicked);
                    e.setCurrentItem(clicked);
                } else {
                    // aus Spielerinventar -> Truhe
                    applyGuiLore(clicked, player, vaultChest);
                    e.setCurrentItem(clicked);
                }
                break;
            }

            // --- Hotbar-Swap (Zahlenkey-Tausch mit angeklicktem Slot)
            case HOTBAR_SWAP: {
                int hotbarSlot = e.getHotbarButton();
                if (hotbarSlot < 0) break;

                ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                // Item im geklickten Slot

                if (rawSlot < topInv.getSize()) {
                    // Slot ist in der Truhe -> nur hier behandeln
                    if (hotbarItem != null && hotbarItem.getType() != Material.AIR) {
                        applyGuiLore(hotbarItem, player, vaultChest); // Hotbar-Item geht in Truhe
                        player.getInventory().setItem(hotbarSlot, hotbarItem);
                    }
                    if (clicked != null && clicked.getType() != Material.AIR) {
                        restoreOriginalLore(clicked); // Truhen-Item geht in Hotbar
                        e.setCurrentItem(clicked);
                    }
                }
                break;
            }

            default:
                break;
        }

        // --- Reine Nummerntaste (ClickType.NUMBER_KEY)
        if (e.getClick() == ClickType.NUMBER_KEY) {
            int hotbarSlot = e.getHotbarButton();
            if (hotbarSlot >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);

                if (rawSlot < topInv.getSize()) {
                    if (hotbarItem != null && hotbarItem.getType() != Material.AIR) {
                        applyGuiLore(hotbarItem, player, vaultChest);
                        player.getInventory().setItem(hotbarSlot, hotbarItem);
                    }
                    if (clicked != null && clicked.getType() != Material.AIR) {
                        restoreOriginalLore(clicked);
                        e.setCurrentItem(clicked);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFilterClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().contains("Filter-Einstellungen")) return;
        if (!(event.getInventory().getHolder() instanceof FilterInventoryHolder holder)) return;

        VaultChest chest = holder.getVaultChest();
        if (chest == null) return;

        manager().stopAutoTask(chest);
        chest.getPendingItems().clear();

        ItemStack[] contents = event.getInventory().getContents();
        for (int i = 0; i < 9; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                chest.getPendingItems().offer(item.clone());
            }
        }

        chest.saveFilter();
        manager().saveChest(chest);

        // Task neu starten (nach GUI schließen)
        Bukkit.getScheduler().runTaskLater(IslandVault.getPlugin(), () ->
                manager().startAutoTask(chest, createChestTask(chest)), 8L);
//TODO aus task Liste vorher entfernen
    }

    // -----------------------------
// Filter-GUI Klicks
// -----------------------------
    @EventHandler
    public void onFilterClick(InventoryClickEvent event) {
        if (!"Filter-Einstellungen".contains(event.getView().getTitle())) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof FilterInventoryHolder filterHolder)) return;

        VaultChest chest = filterHolder.getVaultChest();
        if (chest == null) return;

        int slot = event.getSlot();

        // Toggle-Button (Wechsel Input/Output)
        if (slot == 13) {
            chest.toggleInputOutput();
            event.getInventory().setItem(13, createIoButton(chest.isInputChest())); // Button aktualisieren
            event.setCancelled(true);

            // Änderungen speichern
            IslandVault.getVaultChestManager().saveChest(chest);
            return;
        }

        // Filter-Slots (0–8)
        if (slot >= 0 && slot <= 8) {
            ItemStack cursor = event.getCursor();
            ItemStack clicked = event.getCurrentItem();

            // Linksklick = Filter-Template hinzufügen
            if (event.getClick().isLeftClick() && cursor != null && cursor.getType() != Material.AIR) {
                ItemStack template = cursor.clone();
                template.setAmount(1);

                // Neuen Filter-Eintrag merken
                chest.getPendingItems().offer(template);
                event.getInventory().setItem(slot, template);
                event.setCancelled(true);

                // Änderungen speichern
                chest.saveFilter();
                manager().saveChest(chest);
            }

            // Rechtsklick = Filter-Eintrag entfernen
            else if (event.getClick().isRightClick() && clicked != null && clicked.getType() != Material.AIR) {
                chest.getPendingItems().remove(clicked);
                event.getInventory().setItem(slot, null);
                event.setCancelled(true);

                // Änderungen speichern
                chest.saveFilter();
                manager().saveChest(chest);
            }

            // Linksklick ohne Item = Klick unterbinden (verhindert Item-Verschiebung)
            if (event.getClick().isLeftClick() && (cursor == null || cursor.getType() == Material.AIR)) {
                //event.setCancelled(true);
            }
        }
    }


    // ================================================================
    // ===================== AutoFill / AutoPull ======================
    // ================================================================
    public static BukkitRunnable createChestTask(VaultChest chest) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (chest.isInputChest()) doAutoPull(chest);
                else doAutoFill(chest);
            }
        };
    }

    private static void doAutoFill(VaultChest chest) {
        Inventory inv = chest.getChestInventory();
        if (inv == null) return;

        Queue<ItemStack> queue = chest.getPendingItems();
        for (ItemStack t : chest.getSavedFilter()) {
            if (t != null && t.getType() != Material.AIR && !queueContainsMaterial(queue, t.getType())) {
                queue.offer(t.clone());
            }
        }

        if (queue.isEmpty()) return;
        ItemStack template = queue.peek();
        if (template == null) return;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (slot != null && slot.getType() == template.getType() && slot.getAmount() < slot.getMaxStackSize()) {
                ItemStack pulled = IslandVault.getVaultManager()
                        .getItemFromVault(template.getType(), 1, chest.getIsland().getOwner(), null);
                if (pulled == null) continue;
                slot.setAmount(slot.getAmount() + 1);
                queue.poll();
                updateViewers(chest);
                return;
            } else if (slot == null || slot.getType() == Material.AIR) {
                ItemStack pulled = IslandVault.getVaultManager()
                        .getItemFromVault(template.getType(), 1, chest.getIsland().getOwner(), null);
                if (pulled == null) continue;
                inv.setItem(i, new ItemStack(template.getType(), 1));
                queue.poll();
                updateViewers(chest);
                return;
            }
        }
    }

    private static void doAutoPull(VaultChest chest) {
        Inventory inv = chest.getChestInventory();
        if (inv == null) return;

        PlayerVaultUtils vault = IslandVault.getVaultManager().getVaults().get(chest.getIsland().getOwner());
        if (vault == null) return;

        ItemStack[] filter = chest.getSavedFilter();
        boolean hasFilter = Arrays.stream(filter).anyMatch(i -> i != null && i.getType() != Material.AIR);

        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;

            Material mat = stack.getType();
            if (!vault.getUnlockedMaterial().contains(mat)) continue;

            if (hasFilter && Arrays.stream(filter)
                    .filter(f -> f != null && f.getType() != Material.AIR)
                    .noneMatch(f -> f.getType() == mat)) continue;

            IslandVault.getVaultManager().addItemToVault(mat, 1, chest.getIsland().getOwner(), null);

            if (stack.getAmount() > 1) stack.setAmount(stack.getAmount() - 1);
            else inv.setItem(slot, null);

            updateViewers(chest);
            break;
        }
    }

    private static void updateViewers(VaultChest chest) {
        UUID owner = chest.getIsland().getOwner();
        if (IslandVault.getVaultManager().hasViewers(owner)) {
            IslandVault.getVaultManager().updateViewers(owner);
        }
    }

    private static boolean queueContainsMaterial(Queue<ItemStack> queue, Material mat) {
        for (ItemStack i : queue) if (i != null && i.getType() == mat) return true;
        return false;
    }

    // ================================================================
    // ===================== Hilfsmethoden (Lore, GUI) ================
    // ================================================================
    private void restoreOriginalLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(IslandVault.getPlugin(), "originalLore");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(key, PersistentDataType.STRING)) {
            String loreStr = pdc.get(key, PersistentDataType.STRING);
            List<String> original = (loreStr == null || loreStr.isEmpty()) ? null : Arrays.asList(loreStr.split("§n", -1));
            meta.setLore(original);
            pdc.remove(key);
            item.setItemMeta(meta);
        } else {
            List<String> lore = meta.getLore();
            if (lore != null && !lore.isEmpty()) {
                int idx = indexOfGuiMarker(lore);
                if (idx != -1) {
                    List<String> cleaned = new ArrayList<>(lore.subList(0, idx));
                    meta.setLore(cleaned.isEmpty() ? null : cleaned);
                    item.setItemMeta(meta);
                }
            }
        }
    }

    private int indexOfGuiMarker(List<String> lore) {
        for (int i = 0; i < lore.size(); i++) {
            String raw = ChatColor.stripColor(lore.get(i));
            if (raw != null && raw.startsWith(GUI_MARKER)) return i;
        }
        return -1;
    }

    private void applyGuiLore(ItemStack item, Player player, VaultChest chest) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(IslandVault.getPlugin(), "originalLore");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Schon markiert?
        if (meta.hasLore()) {
            for (String l : meta.getLore()) {
                String raw = ChatColor.stripColor(l);
                if (raw != null && raw.startsWith(GUI_MARKER)) return;
            }
        }

        if (!pdc.has(key, PersistentDataType.STRING)) {
            String original = meta.hasLore() ? String.join("§n", meta.getLore()) : "";
            pdc.set(key, PersistentDataType.STRING, original);
        }

        PlayerVaultUtils vaultUtils = IslandVault.getVaultManager()
                .getVaults()
                .get(IslandUtils.getIslandOwnerUUID(player));

        boolean unlocked = vaultUtils != null && vaultUtils.getUnlockedMaterial().contains(item.getType());
        boolean inFilter = Arrays.stream(chest.getSavedFilter())
                .filter(Objects::nonNull)
                .anyMatch(f -> f.getType() == item.getType());

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (!lore.isEmpty()) lore.add("");
        lore.add("§7" + GUI_MARKER);

        if (IslandVault.getItemManager().materialIsInternalBlacklisted(item.getType())) {
            lore.add("§c☒ Item kann nicht ins Lager");
        } else {
            lore.add(unlocked ? "§a☑ Kann ins Lager" : "§c☒ Nicht freigeschaltet");
            if (!chest.getFilterIndexes().isEmpty()) {
                lore.add(inFilter ? "§a☑ Im Filter enthalten" : "§c☒ Nicht im Filter");
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private ItemStack createIoButton(boolean isInput) {
        ItemStack button = new ItemStack(Material.IRON_TRAPDOOR);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName("§6Modus wählen: §aEinlagerung §6/ §dAuslagerung");
        meta.setLore(Arrays.asList(
                "§eAktueller Modus: " + (isInput ? "§aEinlagerung" : "§dAuslagerung"),
                "§eLinksklick: §bModus ändern"
        ));
        button.setItemMeta(meta);
        return button;
    }
}
