package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.InventoryHolder.FilterInventoryHolder;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Items.VaultChest;
import de.tebrox.islandVault.Items.VaultChestItem;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;

public class VaultChestListener implements Listener {

    private static final String GUI_MARKER = "Füllt sich automatisch ins Lager";

    // -----------------------------
    // Chest platzieren
    // -----------------------------
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        // wenn es eine VaultChest sein soll, nur dann diese Logik anwenden
        if (VaultChestItem.isCustomChest(item)) {
            if (!IslandUtils.hasAccessToCurrentIsland(player)) {
                event.setCancelled(true);
                player.sendMessage("§cDu kannst auf dieser Insel keine Inseltruhen platzieren.");
                return;
            }

            // Registrierung um 1 Tick verschieben
            Bukkit.getScheduler().runTask(IslandVault.getPlugin(), () -> {
                if (!(block.getState() instanceof Chest chestBlock)) return;

                InventoryHolder holder = chestBlock.getInventory().getHolder();
                if (holder instanceof DoubleChest doubleChest) {
                    // Doppeltruhe erkannt → beide Seiten wieder auf SINGLE setzen
                    Chest left = (Chest) doubleChest.getLeftSide();
                    Chest right = (Chest) doubleChest.getRightSide();

                    for (Chest c : new Chest[]{left, right}) {
                        Block cBlock = c.getBlock();
                        if (cBlock.getBlockData() instanceof org.bukkit.block.data.type.Chest cData) {
                            cData.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
                            cBlock.setBlockData(cData, true);
                        }
                    }
                }

                // VaultChest registrieren
                Island island = IslandUtils.getIslandManager().getIslandAt(block.getLocation()).orElse(null);
                VaultChest chest = new VaultChest(player.getUniqueId(), block.getLocation(), island);
                chest.getChestInventory().setContents(chestBlock.getInventory().getContents());
                IslandVault.customChests.put(block.getLocation(), chest);

                player.sendMessage("§aInseltruhe platziert.");
            });
        } else {
            // Normale Chest → trotzdem verhindern, dass sie sich mit einer VaultChest verbindet
            Bukkit.getScheduler().runTask(IslandVault.getPlugin(), () -> {
                if (!(block.getState() instanceof Chest chestBlock)) return;

                InventoryHolder holder = chestBlock.getInventory().getHolder();
                if (holder instanceof DoubleChest doubleChest) {
                    Chest left = (Chest) doubleChest.getLeftSide();
                    Chest right = (Chest) doubleChest.getRightSide();

                    // Prüfen, ob eine der beiden in deiner VaultChest-Map steckt
                    boolean containsVault = IslandVault.customChests.containsKey(left.getLocation())
                            || IslandVault.customChests.containsKey(right.getLocation());

                    if (containsVault) {
                        for (Chest c : new Chest[]{left, right}) {
                            Block cBlock = c.getBlock();
                            if (cBlock.getBlockData() instanceof org.bukkit.block.data.type.Chest cData) {
                                cData.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
                                cBlock.setBlockData(cData, true);
                            }
                        }
                        //player.sendMessage("§eEine Verbindung mit einer Inseltruhe wurde verhindert.");
                    }
                }
            });
        }
    }




    // -----------------------------
    // Chest zerstören
    // -----------------------------
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        VaultChest chest = IslandVault.customChests.get(block.getLocation());
        if (chest == null) return;

        Player player = event.getPlayer();
        if(!IslandUtils.hasAccessToCurrentIsland(player))  {
            event.setCancelled(true);
            player.sendMessage("§cDu darfst diese Truhe nicht zerstören.");
            return;
        }

        event.setDropItems(false);

        // Items droppen
        for (ItemStack item : chest.getChestInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
        }

        // Custom Chest Item droppen
        if (!(event.getPlayer().getGameMode() == GameMode.CREATIVE)) {
            block.getWorld().dropItemNaturally(block.getLocation(), VaultChestItem.createCustomChest());
        }


        // Autofill-Task stoppen
        BukkitRunnable task = IslandVault.runningVaultChestTasks.remove(chest);
        if (task != null) task.cancel();

        IslandVault.customChests.remove(block.getLocation());
        player.sendMessage("§cInseltruhe zerstört.");
    }

    // -----------------------------
    // Interact: Filter-GUI öffnen
    // -----------------------------
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        VaultChest chest = IslandVault.customChests.get(event.getClickedBlock().getLocation());
        if (chest == null) return;

        Player player = event.getPlayer();
        if(!IslandUtils.hasAccessToCurrentIsland(player))  {
            player.sendMessage("§cDu hast keine Berechtigung für die Inseltruhe!");
            return;
        }

        // Sneak + Rechtsklick = Filter-GUI öffnen
        if (player.isSneaking() && event.getAction().isRightClick()) {
            event.setCancelled(true);

            // FilterItems laden
            ItemStack[] filterItems = chest.loadFilter();

            // Holder erzeugen
            FilterInventoryHolder holder = new FilterInventoryHolder(chest, event.getClickedBlock());

            // Inventory erstellen
            Inventory filterInventory = Bukkit.createInventory(holder, 18, "Filter-Einstellungen");
            holder.setInventory(filterInventory);

            // FilterItems ins Inventory setzen
            filterInventory.setContents(filterItems);

            ItemStack ioButton = createIoButton(chest.isInputChest());

            for(int i = 9; i < 18; i++){
                filterInventory.setItem(i, GuiItemUtils.FILLER_GLASS);
            }

            filterInventory.setItem(13, ioButton);

            // Inventory öffnen
            player.openInventory(filterInventory);

            chest.setLastEditor(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        Inventory topInv = e.getView().getTopInventory();
        InventoryHolder holder = topInv.getHolder();
        VaultChest vaultChest = null;
        Location loc = null;
        if (holder instanceof Chest chestBlock) {
            loc = chestBlock.getLocation();
        } else if (holder instanceof DoubleChest doubleChest) {
            loc = doubleChest.getLocation();
        }
        if (loc != null) {
            vaultChest = IslandVault.customChests.get(loc);
        }
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
        InventoryHolder holder = topInv.getHolder();
        VaultChest vaultChest = null;
        Location loc = null;
        if (holder instanceof Chest chestBlock) {
            loc = chestBlock.getLocation();
        } else if (holder instanceof DoubleChest doubleChest) {
            loc = doubleChest.getLocation();
        }
        if (loc != null) {
            vaultChest = IslandVault.customChests.get(loc);
        }

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

        Inventory topInv = e.getView().getTopInventory();       // Truhe/GUI
        int rawSlot = e.getRawSlot();

        // VaultChest anhand Block-Location holen (Chest/DoubleChest)
        VaultChest vaultChest = null;
        InventoryHolder holder = topInv.getHolder();
        Location loc = null;
        if (holder instanceof Chest chestBlock) {
            loc = chestBlock.getLocation();
        } else if (holder instanceof DoubleChest doubleChest) {
            loc = doubleChest.getLocation();
        }
        if (loc != null) {
            vaultChest = IslandVault.customChests.get(loc);
        }
        if (vaultChest == null) return;

        if(!vaultChest.isInputChest()) return;

        ItemStack clicked = e.getCurrentItem(); // Item im Slot (vor der Aktion)
        ItemStack cursor = e.getCursor();      // Item auf dem Cursor (vor der Aktion)

        switch (e.getAction()) {
            // --- Mit der Hand ablegen: wir ändern den CURSOR, denn der wird in den Slot gelegt
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case SWAP_WITH_CURSOR: {
                if (cursor != null && cursor.getType() != Material.AIR) {
                    if (rawSlot < topInv.getSize()) {
                        // Cursor-Item wird gleich in die Truhe gelegt → Lore anhängen
                        applyGuiLore(cursor, player, vaultChest);
                    } else {
                        // Cursor-Item wird ins Spielerinventar gelegt → Original wiederherstellen
                        restoreOriginalLore(cursor);
                    }
                }
                // kein setCursor() und kein setCurrentItem() nötig!
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
                } else {
                    // Quelle: Spielerinventar -> hier NICHT vorsorglich GUI-Lore anhängen
                    // (wir entscheiden erst beim Ablegen in der Truhe -> PLACE_* Zweig)
                }
                break;
            }

            // --- Shift-Klick: Item wandert ins andere Inventar; wir ändern das Quell-Item
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
                ItemStack target = clicked; // Item im geklickten Slot

                if (rawSlot < topInv.getSize()) {
                    // Slot ist in der Truhe -> nur hier behandeln
                    if (hotbarItem != null && hotbarItem.getType() != Material.AIR) {
                        applyGuiLore(hotbarItem, player, vaultChest);                   // Hotbar-Item geht in Truhe
                        player.getInventory().setItem(hotbarSlot, hotbarItem);
                    }
                    if (target != null && target.getType() != Material.AIR) {
                        restoreOriginalLore(target);                                    // Truhen-Item geht in Hotbar
                        e.setCurrentItem(target);
                    }
                } else {
                    // Slot ist im Spielerinventar -> reiner Bottom↔Hotbar Tausch => IGNORIEREN
                }
                break;
            }

            default:
                break;
        }

        // Extra: Reine Nummerntaste (einige Server setzen nur ClickType.NUMBER_KEY)
        if (e.getClick() == ClickType.NUMBER_KEY) {
            int hotbarSlot = e.getHotbarButton();
            if (hotbarSlot >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                ItemStack target = clicked;

                if (rawSlot < topInv.getSize()) {
                    if (hotbarItem != null && hotbarItem.getType() != Material.AIR) {
                        applyGuiLore(hotbarItem, player, vaultChest);
                        player.getInventory().setItem(hotbarSlot, hotbarItem);
                    }
                    if (target != null && target.getType() != Material.AIR) {
                        restoreOriginalLore(target);
                        e.setCurrentItem(target);
                    }
                } else {
                    // Bottom/Hotbar intern -> ignorieren
                }
            }
        }
    }

    private void restoreOriginalLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(IslandVault.getPlugin(), "originalLore");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(key, PersistentDataType.STRING)) {
            String loreString = pdc.get(key, PersistentDataType.STRING);

            // Wenn leerer String gespeichert → keine Lore setzen
            List<String> originalLore = (loreString == null || loreString.isEmpty())
                    ? null
                    : Arrays.asList(loreString.split("§n", -1));

            meta.setLore(originalLore);
            pdc.remove(key); // Original-Lore nach Wiederherstellung entfernen
            item.setItemMeta(meta);

            //IslandVault.getPlugin().getLogger().info("restoreOriginalLore angewendet für " + item.getType());
        } else {
            // Fallback: Wenn aus irgendeinem Grund keine Original-Lore gespeichert war,
            // versuche die GUI-Zeilen anhand des Markers zu entfernen
            List<String> lore = meta.getLore();
            if (lore != null && !lore.isEmpty()) {
                int idx = indexOfGuiMarker(lore);
                if (idx != -1) {
                    List<String> cleaned = new ArrayList<>(lore.subList(0, idx));
                    meta.setLore(cleaned.isEmpty() ? null : cleaned);
                    item.setItemMeta(meta);

                    IslandVault.getPlugin().getLogger().info("restoreOriginalLore Fallback genutzt für " + item.getType());
                }
            }
        }
    }

    private int indexOfGuiMarker(List<String> lore) {
        for (int i = 0; i < lore.size(); i++) {
            String raw = org.bukkit.ChatColor.stripColor(lore.get(i));
            if (raw != null && raw.startsWith("Füllt sich automatisch ins Lager")) {
                return i;
            }
        }
        return -1;
    }


    private void applyGuiLore(ItemStack item, Player player, VaultChest chest) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(IslandVault.getPlugin(), "originalLore");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Bereits GUI-markiert? -> nicht erneut anhängen
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                String raw = org.bukkit.ChatColor.stripColor(line);
                if (raw != null && raw.startsWith(GUI_MARKER)) {
                    item.setItemMeta(meta); // nichts ändern
                    return;
                }
            }
        }

        // Original-Lore einmalig sichern (auch wenn leer)
        if (!pdc.has(key, PersistentDataType.STRING)) {
            String original = meta.hasLore() ? String.join("§n", meta.getLore()) : "";
            pdc.set(key, PersistentDataType.STRING, original);
        }

        // Freischaltung / Filter prüfen
        PlayerVaultUtils vaultUtils = IslandVault.getVaultManager()
                .getVaults()
                .get(IslandUtils.getIslandOwnerUUID(player));
        boolean unlocked = vaultUtils != null && vaultUtils.getUnlockedMaterial().contains(item.getType());

        boolean inFilter = Arrays.stream(chest.getSavedFilter())
                .filter(Objects::nonNull)
                .anyMatch(f -> f.getType() == item.getType());

        // Alte Lore übernehmen, neue Zeilen anhängen
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (!lore.isEmpty()) {
            lore.add(""); // <- Leerzeile zwischen Original und GUI-Lore
        }
        lore.add("§7" + GUI_MARKER);

        if(IslandVault.getItemManager().materialIsInternalBlacklisted(item.getType())) {
            lore.add("§c☒ Item kann nicht ins Lager");
        }else{
            lore.add(unlocked ? "§a☑ Kann ins Lager" : "§c☒ Nicht freigeschaltet");
            if(!chest.getFilterIndexes().isEmpty()) {
                lore.add(inFilter ? "§a☑ Im Filter enthalten" : "§c☒ Nicht im Filter");
            }

        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }



    // -----------------------------
    // Filter-GUI schließen
    // -----------------------------
    @EventHandler
    public void onFilterClose(InventoryCloseEvent event) {
        if (!"Filter-Einstellungen".contains(event.getView().getTitle())) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof FilterInventoryHolder filterHolder)) return;

        VaultChest chest = filterHolder.getVaultChest();
        if (chest == null) return;

        BukkitRunnable oldTask = IslandVault.runningVaultChestTasks.remove(chest);
        if (oldTask != null) oldTask.cancel();

        // PendingItems aus GUI aktualisieren
        chest.getPendingItems().clear();

        ItemStack[] tempInv = event.getInventory().getContents();
        for(int i = 0; i < 9; i++) {
            ItemStack item = tempInv[i];
            if (item != null && item.getType() != Material.AIR) {
                chest.getPendingItems().offer(item.clone());
            }
        }

        // Filter speichern
        chest.saveFilter();
        IslandVault.getPlugin().saveChests();

        // Autofill starten
        startChestTask(chest);
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
        // Toggle-Button
        if (slot == 13) {
            chest.toggleInputOutput();
            event.getInventory().setItem(13, createIoButton(chest.isInputChest())); // Button aktualisieren
            event.setCancelled(true);
            return;
        }


        if(slot >= 0 && slot <= 8) {
            ItemStack cursor = event.getCursor();
            ItemStack clicked = event.getCurrentItem();

            // Linksklick = Template setzen
            if (event.getClick().isLeftClick() && cursor != null && cursor.getType() != Material.AIR) {
                ItemStack template = cursor.clone();
                template.setAmount(1);
                chest.getPendingItems().offer(template);
                event.getInventory().setItem(slot, template);
                event.setCancelled(true);
            }

            // Rechtsklick = Template entfernen
            else if (event.getClick().isRightClick() && clicked != null && clicked.getType() != Material.AIR) {
                chest.getPendingItems().remove(clicked);
                event.getInventory().setItem(slot, null);
                event.setCancelled(true);
            }

            if(event.getClick().isLeftClick() && cursor != null && cursor.getType() == Material.AIR) {
                event.setCancelled(true);
            }
        }
    }

    // -----------------------------
    // Autofill Task
    // -----------------------------
    /**public static void startAutofillTask(VaultChest chest) {
        if (IslandVault.runningVaultChestTasks.containsKey(chest)) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Inventory chestInv = chest.getChestInventory();
                if (chestInv == null) return;

                Queue<ItemStack> queue = chest.getPendingItems();

                // 1️⃣ Prüfe Filter-Items und füge Items in die Queue, falls noch nicht vorhanden
                for (ItemStack template : chest.getSavedFilter()) {
                    if (template != null && template.getType() != Material.AIR && !queueContainsMaterial(queue, template.getType())) {
                        queue.offer(template.clone());
                    }
                }

                // 2️⃣ Queue leer → idle weiterlaufen
                if (queue.isEmpty()) return;

                // 3️⃣ Nächstes Item vorbereiten
                ItemStack template = queue.peek(); // NICHT entfernen, noch nicht aus Vault nehmen
                if (template == null) return;

                int amountToPlace = template.getAmount();
                boolean placedSomething = false;

                // 4️⃣ Erst gleiche Items stapeln
                for (int i = 0; i < chestInv.getSize(); i++) {
                    ItemStack slot = chestInv.getItem(i);
                    if (slot != null && slot.getType() == template.getType()) {
                        int space = slot.getMaxStackSize() - slot.getAmount();
                        if (space > 0) {
                            int add = Math.min(space, amountToPlace);
                            // Erst jetzt Vault anfragen
                            ItemStack next = IslandVault.getVaultManager().getItemFromVault(template.getType(), add, chest.getIsland().getOwner(), null);
                            if (next == null || next.getAmount() <= 0) continue;

                            slot.setAmount(slot.getAmount() + next.getAmount());
                            amountToPlace -= next.getAmount();
                            placedSomething = true;

                            if (amountToPlace <= 0) break;
                        }
                    }
                }

                // 5️⃣ Freie Slots verwenden
                if (amountToPlace > 0) {
                    for (int i = 0; i < chestInv.getSize(); i++) {
                        ItemStack slot = chestInv.getItem(i);
                        if (slot == null || slot.getType() == Material.AIR) {
                            int add = Math.min(template.getMaxStackSize(), amountToPlace);
                            ItemStack next = IslandVault.getVaultManager().getItemFromVault(template.getType(), add, chest.getIsland().getOwner(), null);
                            if (next == null || next.getAmount() <= 0) continue;

                            ItemStack toPlace = next.clone();
                            chestInv.setItem(i, toPlace);
                            amountToPlace -= next.getAmount();
                            placedSomething = true;

                            if (amountToPlace <= 0) break;
                        }
                    }
                }

                // 6️⃣ Items, die nicht platziert werden konnten, bleiben in der Queue
                if (amountToPlace <= 0) {
                    queue.poll(); // Item wurde komplett platziert → aus Queue entfernen
                }

                if (placedSomething) {
                    Bukkit.getLogger().info("[AutoFill] Items aus Vault in Chest bei " + chest.getLocation() + " platziert: " + template.getType());
                    if(IslandVault.getVaultManager().hasViewers(chest.getIsland().getOwner())) {
                        IslandVault.getVaultManager().updateViewers(chest.getIsland().getOwner());
                    }
                }
            }

            private boolean queueContainsMaterial(Queue<ItemStack> queue, Material mat) {
                for (ItemStack item : queue) {
                    if (item != null && item.getType() == mat) return true;
                }
                return false;
            }
        };

        task.runTaskTimer(IslandVault.getPlugin(), 0L, 10L); // alle 0,5 Sekunden prüfen
        IslandVault.runningVaultChestTasks.put(chest, task);
    }
     **/

    public static void startChestTask(VaultChest chest) {
        if (IslandVault.runningVaultChestTasks.containsKey(chest)) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (chest.isInputChest()) {
                    doAutoPull(chest);
                } else {
                    doAutoFill(chest);
                }
            }
        };

        // gleicher Intervall für beide Richtungen
        task.runTaskTimer(IslandVault.getPlugin(), 0L, 8L); // 1 Sekunde = 20 Ticks
        IslandVault.runningVaultChestTasks.put(chest, task);
    }

    private static void doAutoFill(VaultChest chest) {
        Inventory chestInv = chest.getChestInventory();
        if (chestInv == null) return;

        Queue<ItemStack> queue = chest.getPendingItems();

        // Filter prüfen und Queue füllen
        for (ItemStack template : chest.getSavedFilter()) {
            if (template != null && template.getType() != Material.AIR && !queueContainsMaterial(queue, template.getType())) {
                queue.offer(template.clone());
            }
        }

        if (queue.isEmpty()) return;

        ItemStack template = queue.peek();
        if (template == null) return;

        int amountToPlace = 1; // nur 1 Item pro Tick
        boolean placedSomething = false;

        // Stapeln oder in freien Slot legen
        for (int i = 0; i < chestInv.getSize(); i++) {
            ItemStack slot = chestInv.getItem(i);

            if (slot != null && slot.getType() == template.getType() && slot.getAmount() < slot.getMaxStackSize()) {
                ItemStack next = IslandVault.getVaultManager().getItemFromVault(template.getType(), 1, chest.getIsland().getOwner(), null);
                if (next == null) continue;

                slot.setAmount(slot.getAmount() + 1);
                placedSomething = true;
                amountToPlace--;
                break;
            }

            if (slot == null || slot.getType() == Material.AIR) {
                ItemStack next = IslandVault.getVaultManager().getItemFromVault(template.getType(), 1, chest.getIsland().getOwner(), null);
                if (next == null) continue;

                chestInv.setItem(i, new ItemStack(template.getType(), 1));
                placedSomething = true;
                amountToPlace--;
                break;
            }
        }

        if (amountToPlace <= 0) queue.poll();

        if (placedSomething) {
            //Bukkit.getLogger().info("[AutoFill] 1x " + template.getType() + " in Chest bei " + chest.getLocation() + " gelegt.");
            if (IslandVault.getVaultManager().hasViewers(chest.getIsland().getOwner())) {
                IslandVault.getVaultManager().updateViewers(chest.getIsland().getOwner());
            }
        }
    }


    private static void doAutoPull(VaultChest chest) {
        Inventory chestInv = chest.getChestInventory();
        if (chestInv == null) return;

        PlayerVaultUtils vaultUtils = IslandVault.getVaultManager().getVaults().get(chest.getIsland().getOwner());
        if (vaultUtils == null) return;

        ItemStack[] filter = chest.getSavedFilter();
        boolean hasFilter = Arrays.stream(filter).anyMatch(i -> i != null && i.getType() != Material.AIR);

        // Slots prüfen
        for (int slot = 0; slot < chestInv.getSize(); slot++) {
            ItemStack stack = chestInv.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;

            Material mat = stack.getType();

            // nur freigeschaltete Materialien
            if (!vaultUtils.getUnlockedMaterial().contains(mat)) continue;

            // nur Items aus Filter, falls gesetzt
            if (hasFilter) {
                boolean allowed = Arrays.stream(filter)
                        .filter(f -> f != null && f.getType() != Material.AIR)
                        .anyMatch(f -> f.getType() == mat);
                if (!allowed) continue;
            }

            // 1 Item ins Vault verschieben
            IslandVault.getVaultManager().addItemToVault(mat, 1, chest.getIsland().getOwner(), null);

            if (stack.getAmount() > 1) {
                stack.setAmount(stack.getAmount() - 1);
            } else {
                chestInv.setItem(slot, null);
            }

            //Bukkit.getLogger().info("[AutoPull] 1x " + mat + " aus Chest bei " + chest.getLocation() + " ins Vault verschoben.");

            if (IslandVault.getVaultManager().hasViewers(chest.getIsland().getOwner())) {
                IslandVault.getVaultManager().updateViewers(chest.getIsland().getOwner());
            }

            break; // nur 1 Item pro Tick
        }
    }

    private static boolean queueContainsMaterial(Queue<ItemStack> queue, Material mat) {
        for (ItemStack item : queue) {
            if (item != null && item.getType() == mat) return true;
        }
        return false;
    }

    private ItemStack createIoButton(boolean isInput) {
        ItemStack button = new ItemStack(Material.IRON_TRAPDOOR);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName("§6Modus wählen: §aEinlagerung §6/ §dAuslagerung");

        List<String> lore = new ArrayList<>();
        lore.add("§eAktueller Modus: " + (isInput ? "§aEinlagerung": "§dAuslagerung"));
        lore.add("§eLinksklick: §bModus ändern");
        meta.setLore(lore);

        button.setItemMeta(meta);
        return button;
    }
}
