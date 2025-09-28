package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Items.VaultChest;
import de.tebrox.islandVault.Items.VaultChestItem;
import de.tebrox.islandVault.Utils.IslandUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class VaultChestListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (VaultChestItem.isCustomChest(item)) {
            Player player = event.getPlayer();
            Block block = event.getBlockPlaced();

            try {
                Chest chestBlock = (Chest) block.getState();
                Island island = IslandUtils.getIslandManager().getIslandAt(block.getLocation()).orElse(null);
                VaultChest chest = new VaultChest(player.getUniqueId(), block.getLocation(), island);
                chest.getInventory().setContents(chestBlock.getInventory().getContents());
                IslandVault.customChests.put(block.getLocation(), chest);
                player.sendMessage("§aInseltruhe platziert.");
            } catch (ClassCastException e) {
                player.sendMessage("§cFehler: Block ist keine Chest.");
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        VaultChest chest = IslandVault.customChests.get(block.getLocation());
        if (chest == null) return;

        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(chest.getOwner())) {
            event.setCancelled(true);
            player.sendMessage("§cDu darfst diese Truhe nicht zerstören.");
            return;
        }

        // Items droppen
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
        }

        // Custom Chest Item droppen
        block.getWorld().dropItemNaturally(block.getLocation(), VaultChestItem.createCustomChest());

        // Queue stoppen
        BukkitRunnable task = IslandVault.runningVaultChestTasks.remove(chest);
        if (task != null) task.cancel();

        // Entfernen
        IslandVault.customChests.remove(block.getLocation());
        player.sendMessage("§cInseltruhe zerstört.");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        VaultChest chest = IslandVault.customChests.get(event.getClickedBlock().getLocation());
        if (chest == null) return;

        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(chest.getOwner())) {
            player.sendMessage("§cDu bist nicht der Eigentümer dieser Inseltruhe!");
            return;
        }

        // GUI nur bei Sneak öffnen, sonst normalen Abbau erlauben
        if (player.isSneaking()) {
            event.setCancelled(true);

            Inventory filterInventory = Bukkit.createInventory(null, 9, "Filter-Einstellungen");
            ItemStack[] filterItems = chest.loadFilter();
            for (int i = 0; i < filterItems.length; i++) {
                filterInventory.setItem(i, filterItems[i]);
            }
            player.openInventory(filterInventory);
            chest.setLastEditor(player.getUniqueId());

        }
    }

    @EventHandler
    public void onFilterClose(InventoryCloseEvent event) {
        if (!"Filter-Einstellungen".equals(event.getView().getTitle())) return;

        if(event.getInventory().getType().equals(InventoryType.CHEST)) {
            //Chest chest = (Chest) event.getInventory().getHolder();


            Location loc = event.getInventory().getHolder().getInventory().getLocation();
            System.out.println("Chestlocation: " + loc.toBlockLocation());

            VaultChest vaultChest = IslandVault.customChests.get(loc);
            if (vaultChest == null) return;

            // PendingItems aktualisieren
            vaultChest.getPendingItems().clear();
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    vaultChest.getPendingItems().offer(item.clone());
                }
            }

            // Filter persistent speichern
            vaultChest.saveFilter();
            IslandVault.getPlugin().saveChests();

            // Autocollect-Task starten
            startAutofillTask(vaultChest, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
        }
    }

    public static void startAutofillTask(VaultChest chest, List<Integer> filterIndexes) {
        if (IslandVault.runningVaultChestTasks.containsKey(chest)) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Queue<ItemStack> queue = chest.getPendingItems();
                if (queue.isEmpty()) {
                    cancel();
                    IslandVault.runningVaultChestTasks.remove(chest);
                    return;
                }

                ItemStack template = queue.poll();
                if (template == null) return;

                ItemStack next = IslandVault.getVaultManager().getItemFromVault(template.getType(), 1, chest.getIsland().getOwner(), null);
                if (next == null) {
                    queue.offer(template);
                    return;
                }

                // AutoCollect: Items aus dem Filter in freie Slots der Truhe legen
                for (int i : filterIndexes) {
                    ItemStack slot = chest.getInventory().getItem(i);
                    if (slot == null || slot.getType() == Material.AIR) {
                        chest.getInventory().setItem(i, next);
                        return;
                    }
                }

                // Falls voll, wieder in Queue zurück
                queue.offer(template);
            }
        };

        task.runTaskTimer(IslandVault.getPlugin(), 0L, 1L);
        IslandVault.runningVaultChestTasks.put(chest, task);
    }

    @EventHandler
    public void onFilterClick(InventoryClickEvent event) {
        if (!"Filter-Einstellungen".equals(event.getView().getTitle())) return;

        Player player = (Player) event.getWhoClicked();
        VaultChest chest = IslandVault.customChests.values().stream()
                .filter(c -> player.getUniqueId().equals(c.getLastEditor()))
                .findFirst()
                .orElse(null);
        if (chest == null) return;

        int slot = event.getSlot();
        if (slot < 0 || slot >= 9) return; // nur Filter-Slots

        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        // Linksklick = Template setzen
        if (event.getClick().isLeftClick() && cursor != null && cursor.getType() != Material.AIR) {
            ItemStack template = cursor.clone();
            template.setAmount(1); // nur 1 Item als Template
            chest.getPendingItems().offer(template);
            event.getInventory().setItem(slot, template); // GUI-Kopie

            event.setCancelled(true); // verhindert, dass Cursor reduziert wird
        }

        // Rechtsklick = Template entfernen
        else if (event.getClick().isRightClick() && clicked != null && clicked.getType() != Material.AIR) {
            chest.getPendingItems().remove(clicked);
            event.getInventory().setItem(slot, null);
            event.setCancelled(true);
        }

        // Andere Klicks blocken
        else {
            event.setCancelled(true);
        }
    }

}
