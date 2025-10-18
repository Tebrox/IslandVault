package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import de.tebrox.islandVault.Utils.PyrofishingUtils;
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

public class ItemAutoCollectListener implements Listener {

    private final IslandVault plugin;
    private final NamespacedKey dropMarkerKey;

    public ItemAutoCollectListener(IslandVault plugin) {
        this.plugin = plugin;
        this.dropMarkerKey = new NamespacedKey(plugin, "player_dropped");
    }

    // Spielerwirft selbst etwas weg → markiere, damit es nicht eingesammelt wird
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.getItemDrop().getPersistentDataContainer().set(dropMarkerKey, PersistentDataType.INTEGER, 1);
    }

    // Wenn ein Item gespawnt wird → prüfen, ob AutoCollect greifen soll
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        PersistentDataContainer data = item.getPersistentDataContainer();
        if (data.has(dropMarkerKey, PersistentDataType.INTEGER)) return; // Spieler-Drop ignorieren

        Location loc = item.getLocation();
        Optional<Island> optIsland = BentoBox.getInstance().getIslands().getIslandAt(loc);
        if (optIsland.isEmpty()) return;

        Island island = optIsland.get();

        // leicht verzögert, damit das Item stabil existiert
        Bukkit.getScheduler().runTaskLater(plugin, () -> tryAutoCollect(item, island), 2L);
    }

    // Hauptlogik: versuche Item in Insel-Vault einzusammeln
    private void tryAutoCollect(Item item, Island island) {
        if (!item.isValid() || item.isDead()) return;

        ItemStack baseStack = item.getItemStack();
        if (baseStack == null || baseStack.getType() == Material.AIR) return;
        if (PyrofishingUtils.isPyroFishingItem(baseStack)) return;

        // Insel-ID (String, BentoBox)
        String islandId = island.getUniqueId();

        // Prüfen, ob AutoCollect für diese Insel aktiv ist
        if (!IslandVault.getVaultManager().autoCollectIsEnabled(islandId)) return;

        // Sammelradius über Permission-System (Inselbesitzer)
        UUID ownerUUID = island.getOwner();
        if (ownerUUID == null) return;
        int radius = LuckPermsUtils.getMaxRadiusFromPermissions(ownerUUID);
        if (radius <= 0) return;

        Location center = island.getCenter();
        Location loc = item.getLocation();

        // Nur XZ-Abstand prüfen
        double dx = loc.getX() - center.getX();
        double dz = loc.getZ() - center.getZ();
        double distSquared = dx * dx + dz * dz;
        if (distSquared > radius * radius) return;

        // Höhe prüfen
        int y = loc.getBlockY();
        if (y < -64 || y > 319) return;

        // PermissionKey bestimmen
        String permKey = IslandVault.getItemManager().buildPermissionKey(baseStack);

        // Prüfen, ob Inseleigentümer oder Team Zugriff hat (per Gruppe oder Item)
        boolean hasGroupPermission = false;
        List<String> groups = ItemGroupManager.findGroupsWithMaterial(baseStack.getType().toString());
        for (String g : groups) {
            if (LuckPermsUtils.hasPermissionForGroup(ownerUUID, g)) {
                hasGroupPermission = true;
                break;
            }
        }

        if (!LuckPermsUtils.hasPermission(ownerUUID, permKey) && !hasGroupPermission) {
            return;
        }

        // Ähnliche Items im Umkreis zusammenfassen (Stack-Merge)
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

        // Jetzt in Insel-Vault hinzufügen (PermissionKey-basiert)
        IslandVault.getVaultManager().addItemToIslandVault(baseStack, baseStack.getAmount(), islandId, null);

        // Entferne das Item aus der Welt
        item.remove();
    }
}
