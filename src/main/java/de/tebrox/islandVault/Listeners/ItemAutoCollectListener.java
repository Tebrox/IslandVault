package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import de.tebrox.islandVault.PermissionItemRegistry;
import de.tebrox.islandVault.Utils.ItemStackKey;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import de.tebrox.islandVault.VaultData;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;

/**
 * Listener for automatically collecting items dropped in an island.
 * <p>
 * Marks player-dropped items to avoid auto-collecting them,
 * and attempts to auto-collect eligible items near islands after they spawn.
 */
public class ItemAutoCollectListener implements Listener {
    private final IslandVault plugin;
    private final NamespacedKey dropMarkerKey;

    /**
     * Constructs a new ItemAutoCollectListener.
     *
     * @param plugin the main plugin instance
     */
    public ItemAutoCollectListener(IslandVault plugin) {
        this.plugin = plugin;
        dropMarkerKey = new NamespacedKey(plugin, "player_dropped");
    }

    /**
     * Marks items dropped by players with persistent data to prevent auto-collection.
     *
     * @param event the player drop item event
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.getItemDrop().getPersistentDataContainer().set(dropMarkerKey, PersistentDataType.INTEGER, 1);
    }

    /**
     * When an item spawns, attempts to auto-collect it into the island's vault
     * unless it was player-dropped or outside an island.
     * <p>
     * Runs the collection attempt asynchronously with a short delay.
     *
     * @param event the item spawn event
     */
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        PersistentDataContainer data = item.getPersistentDataContainer();

        if (data.has(dropMarkerKey, PersistentDataType.INTEGER)) return;

        Location itemLocation = item.getLocation();

        Optional<Island> optIsland = BentoBox.getInstance().getIslands().getIslandAt(itemLocation);
        if (optIsland.isEmpty()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> tryAutoCollect(item, optIsland.get()), 1L); // 5 Ticks Verzögerung (~0,25 Sek.)
    }

    /**
     * Tries to auto-collect a spawned item into the island's vault,
     * considering permissions, location, and grouping rules.
     *
     * @param item   the item entity to collect
     * @param island the island where the item is located
     */
    private void tryAutoCollect(Item item, Island island) {
        if(!item.isValid() || item.isDead() || IslandVault.getRegionManager().isInAnyRegion(island.getUniqueId(), item.getLocation())) {
            return;
        }
        UUID ownerUUID = island.getOwner();
        if (ownerUUID == null) return;

        VaultData vaultData = IslandVault.getVaultManager().getCachedVault(island.getUniqueId());
        if(!vaultData.getAutoCollect()) {
            return;
        }
        int radius = LuckPermsUtils.getMaxRadiusFromPermissions(ownerUUID);
        if (radius <= 0) return;
        var center = island.getCenter();
        if (item.getLocation().distanceSquared(center) > radius * radius) return;

        ItemStack baseStack = item.getItemStack();
        ItemStackKey key = ItemStackKey.of(baseStack);
        String itemID = IslandVault.getPermissionItemRegistry().getId(key).orElse("");

        boolean groupPermission = false;
        List<String> groups = ItemGroupManager.findGroupsWithItemID(itemID);
        if(!groups.isEmpty()) {
            for(String g : groups) {
                if(LuckPermsUtils.hasPermissionForGroup(ownerUUID, g)) {
                    groupPermission = true;
                    break;
                }
            }
        }

        if (!LuckPermsUtils.hasPermissionForItem(ownerUUID, itemID) && !groupPermission) return;
        List<Item> nearbyItems = item.getWorld().getNearbyEntities(item.getLocation(), 1, 1, 1).stream()
                .filter(e -> e instanceof Item)
                .map(e -> (Item) e)
                .filter(i -> i.isValid() && !i.equals(item) && i.getItemStack().isSimilar(baseStack))
                .toList();

        for (Item nearby : nearbyItems) {
            ItemStack nearbyStack = nearby.getItemStack();
            int transferable = Math.min(nearbyStack.getAmount(), baseStack.getMaxStackSize() - baseStack.getAmount());
            if (transferable > 0) {
                baseStack.setAmount(baseStack.getAmount() + transferable);
                nearbyStack.setAmount(nearbyStack.getAmount() - transferable);
                if (nearbyStack.getAmount() <= 0) nearby.remove();
            }
            if (baseStack.getAmount() >= baseStack.getMaxStackSize()) break;
        }

        vaultData.add(baseStack, baseStack.getAmount());

        item.remove();
    }
}
