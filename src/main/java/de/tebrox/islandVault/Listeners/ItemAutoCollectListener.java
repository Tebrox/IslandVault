package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
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
import java.util.stream.Collectors;

public class ItemAutoCollectListener implements Listener {
    private IslandVault plugin;
    private NamespacedKey dropMarkerKey;

    public ItemAutoCollectListener(IslandVault plugin) {
        this.plugin = plugin;
        dropMarkerKey = new NamespacedKey(plugin, "player_dropped");
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.getItemDrop().getPersistentDataContainer().set(dropMarkerKey, PersistentDataType.INTEGER, 1);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        PersistentDataContainer data = item.getPersistentDataContainer();

        if (data.has(dropMarkerKey, PersistentDataType.INTEGER)) return;

        Location itemLocation = item.getLocation();
        World world = itemLocation.getWorld();

        Optional<Island> optIsland = BentoBox.getInstance().getIslands().getIslandAt(itemLocation);
        if (optIsland.isEmpty()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> tryAutoCollect(item, optIsland.get()), 20L); // 5 Ticks Verzögerung (~0,25 Sek.)
    }

    private void tryAutoCollect(Item item, Island island) {
        if(!item.isValid() || item.isDead()) {
            return;
        }

        UUID ownerUUID = island.getOwner();
        if (ownerUUID == null) return;

        if(!IslandVault.getVaultManager().autoCollectIsEnabled(ownerUUID)) {
            return;
        }

        int radius = LuckPermsUtils.getMaxRadiusFromPermissions(ownerUUID);
        if (radius <= 0) return;

        var center = island.getCenter();
        if (item.getLocation().distanceSquared(center) > radius * radius) return;

        ItemStack baseStack = item.getItemStack();

        if (!LuckPermsUtils.hasPermissionForItem(ownerUUID, baseStack.getType())) return;

        // Suche nach ähnlichen Items im Radius von 1 Block zum Zusammenlegen
        List<Item> nearbyItems = item.getWorld().getNearbyEntities(item.getLocation(), 1, 1, 1).stream()
                .filter(e -> e instanceof Item)
                .map(e -> (Item) e)
                .filter(i -> i.isValid() && !i.equals(item) && i.getItemStack().isSimilar(baseStack))
                .collect(Collectors.toList());

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

        IslandVault.getVaultManager().addItemToVault(baseStack.getType(), baseStack.getAmount(), ownerUUID, null);
        Player owner = Bukkit.getPlayer(island.getOwner());

        if(owner != null) {
            //owner.sendMessage(ChatColor.GREEN + "[IslandVault] " + plugin.getLanguageManager().translate(owner, "collectItemSuccessfull", Map.of("item", baseStack.getI18NDisplayName(), "amount", String.valueOf(baseStack.getAmount()))));
        }

        item.remove();
    }
}
