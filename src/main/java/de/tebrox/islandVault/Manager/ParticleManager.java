package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Tasks.BoxTask;
import de.tebrox.islandVault.Tasks.ParticleTask;
import de.tebrox.islandVault.Tasks.RadiusTask;
import de.tebrox.islandVault.Utils.IslandUtils;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.database.objects.Island;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticleManager {

    private final JavaPlugin plugin;

    private final Map<UUID, Map<String, ParticleTask>> tasks = new HashMap<>();

    public ParticleManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void showBox(Player player, String id, Location pos1, Location pos2, Particle particle, @Nullable Color colorFrom, @Nullable Color colorTo, boolean show) {
        stop(player, id); // ersetzt bestehende Anzeige

        ParticleTask task = new BoxTask(player, pos1, pos2, particle, colorFrom, colorTo, show);
        task.runTaskTimer(plugin, 0L, 4L);

        tasks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(id, task);
    }

    public void showRadiusBorder(Player viewer, String id, Location center, int seconds, Particle particle, @Nullable Color colorFrom, @Nullable Color colorTo) {
        stop(viewer, id);

        Island island = IslandUtils.getIslandManager().getIslandAt(center).orElse(null);
        if (island == null) {
            viewer.sendMessage(IslandVault.getLanguageManager().translate(viewer, "islandNotFound"));
            return;
        }

        UUID ownerUUID = island.getOwner();
        Location centerBlock = island.getCenter();
        int radius = LuckPermsUtils.getMaxRadiusFromPermissions(ownerUUID);

        if (radius <= 0) {
            viewer.sendMessage(IslandVault.getLanguageManager().translate(viewer, "noRadiusPermission"));
            return;
        }

        int minX = island.getMinX();
        int maxX = island.getMaxX();
        int minZ = island.getMinZ();
        int maxZ = island.getMaxZ();
        int minY = island.getY() - 5;
        int maxY = island.getY() + 5;

        RadiusTask task = new RadiusTask(viewer, centerBlock, radius, minX, maxX, minZ, maxZ, minY, maxY, seconds, particle, colorFrom, colorTo);
        task.runTaskTimer(plugin, 0L, 2L);

        tasks.computeIfAbsent(viewer.getUniqueId(), k -> new HashMap<>()).put(id, task);
    }

    public void stop(Player player, String id) {
        Map<String, ParticleTask> map = tasks.get(player.getUniqueId());
        if (map != null) {
            ParticleTask task = map.remove(id);
            if (task != null) task.cancel();

            if (map.isEmpty()) tasks.remove(player.getUniqueId());
        }
    }

    public void stopAll(Player player) {
        Map<String, ParticleTask> map = tasks.remove(player.getUniqueId());
        if (map != null) map.values().forEach(BukkitRunnable::cancel);
    }

    public void stopAll() {
        for (Map<String, ParticleTask> playerTasks : tasks.values()) {
            for (BukkitRunnable task : playerTasks.values()) {
                task.cancel();
            }
            playerTasks.clear();
        }
        tasks.clear();
    }

    public boolean isRunning(Player player, String id) {
        return tasks.containsKey(player.getUniqueId()) && tasks.get(player.getUniqueId()).containsKey(id);
    }
}