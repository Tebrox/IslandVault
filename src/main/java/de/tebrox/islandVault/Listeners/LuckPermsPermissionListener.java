package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.Events.IslandPermissionsUpdateEvent;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;
import java.util.UUID;

public class LuckPermsPermissionListener implements Listener {

    private final Addon addon;
    private final Plugin plugin;

    public LuckPermsPermissionListener(Plugin plugin) {
        this.plugin = plugin;
        this.addon = BentoBox.getInstance().getAddonsManager().getAddonByName("AOneBlock").orElse(null);
        if (addon == null) {
            plugin.getLogger().severe("AOneBlock-Addon nicht gefunden – Permission-Listener wird nicht registriert.");
            return;
        }

        EventBus eventBus = LuckPermsProvider.get().getEventBus();
        eventBus.subscribe(NodeAddEvent.class, this::onPermissionAdded);
        eventBus.subscribe(NodeRemoveEvent.class, this::onPermissionRemoved);
    }

    private void handlePermissionChange(PermissionHolder target, Node node) {
        if (!(target instanceof User user)) {
            // Nicht User, z.B. Gruppe, ignorieren
            return;
        }

        UUID uuid = user.getUniqueId();
        String key = node.getKey();

        if (!key.startsWith("islandvault.vault.") && !key.startsWith("islandvault.groups.")) return;

        Optional<Island> islandOpt = findIslandByOwner(uuid);
        if (islandOpt.isEmpty()) return;

        Island island = islandOpt.get();
        String islandId = island.getUniqueId();

        Bukkit.getScheduler().runTask(plugin, () -> {
            IslandPermissionsUpdateEvent updateEvent = new IslandPermissionsUpdateEvent(uuid, islandId);
            Bukkit.getPluginManager().callEvent(updateEvent);
        });
    }

    private void onPermissionAdded(NodeAddEvent event) {
        handlePermissionChange(event.getTarget(), event.getNode());
    }

    private void onPermissionRemoved(NodeRemoveEvent event) {
        handlePermissionChange(event.getTarget(), event.getNode());
    }

    private Optional<Island> findIslandByOwner(UUID ownerUUID) {
        // Beispiel: addon.getIslands().getAllIslands() liefert Collection<Island>
        return addon.getIslands().getIslands().stream()
                .filter(island -> ownerUUID.equals(island.getOwner()))
                .findFirst();
    }

}
