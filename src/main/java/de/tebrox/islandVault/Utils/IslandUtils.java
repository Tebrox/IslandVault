package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.IslandVault;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class IslandUtils {
    //private static boolean bentoBoxAvailable = false;
    private static final Map<UUID, BukkitRunnable> activeTimers = new HashMap<>();

    public static IslandsManager getIslandManager() {
        return BentoBox.getInstance().getIslands();
    }

    public static UUID getIslandOwnerUUID(Player player) {
        IslandsManager manager = getIslandManager();
        if(manager == null) return null;

        Optional<Island> islandOptional = manager.getIslandAt(player.getLocation());
        if(islandOptional.isPresent()) {
            Island island = islandOptional.get();

            return island.getOwner();
        }

        return null;
    }

    public static boolean isOnOwnIsland(Player player) {
        IslandsManager manager = getIslandManager();
        if (manager == null) return false;

        Optional<Island> islandOptional = manager.getIslandAt(player.getLocation());

        if(islandOptional.isPresent()) {
            Island atLocation = islandOptional.get();
            Island ownIsland = manager.getIsland(player.getWorld(), player.getUniqueId());

            return atLocation != null && atLocation.equals(ownIsland);
        }

        return false;
    }

    public static boolean isOnIslandWhereOwner(Player player) {
        IslandsManager manager = getIslandManager();

        Optional<Island> islandOptional = manager.getIslandAt(player.getLocation());
        if(islandOptional.isPresent()) {
            Island atLocation = islandOptional.get();

            return atLocation != null && atLocation.getOwner().equals(player.getUniqueId());
        }

        return false;
    }

    public static boolean isOnIslandWhereMember(Player player) {
        IslandsManager manager = getIslandManager();

        Optional<Island> islandOptional = manager.getIslandAt(player.getLocation());
        if(islandOptional.isPresent()) {
            Island atLocation = islandOptional.get();

            return atLocation != null && atLocation.getMemberSet().contains(player.getUniqueId());
        }

        return false;
    }

    public static void showRadiusParticles(Player viewer, Location center, int seconds) {
        Island island = IslandUtils.getIslandManager().getIslandAt(center).orElse(null);

        if (island == null) {
            viewer.sendMessage(IslandVault.getLanguageManager().translate(viewer, "islandNotFound"));
            return;
        }

        // Inselbesitzer
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

        if(isTimerActive(viewer)) {
            stopTimer(viewer);
        }

        BukkitRunnable task = new BukkitRunnable() {
            int ticksPassed = 0;
            int totalTicks = seconds * 20;

            final Particle particle = Particle.HAPPY_VILLAGER;
            final int cx = centerBlock.getBlockX();
            final int cz = centerBlock.getBlockZ();

            @Override
            public void run() {
                if (ticksPassed >= totalTicks) {
                    viewer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(IslandVault.getLanguageManager().translate(viewer, "radiusActionbarTimer", Map.of("secondsLeft", String.valueOf(0)))));
                    this.cancel();
                    activeTimers.remove(viewer.getUniqueId());
                    return;
                }

                for (int y = minY; y <= maxY; y++) {

                    int x1 = Math.max(cx - radius, minX);
                    int x2 = Math.min(cx + radius, maxX);
                    int z1 = Math.max(cz - radius, minZ);
                    int z2 = Math.min(cz + radius, maxZ);

                    // Linie entlang X bei z1
                    for (int x = x1; x <= x2; x++) {
                        viewer.spawnParticle(particle, x + 0.5, y + 0.1, z1 + 0.5, 0, 0, 0, 0, 0);
                    }

                    // Linie entlang X bei z2
                    for (int x = x1; x <= x2; x++) {
                        viewer.spawnParticle(particle, x + 0.5, y + 0.1, z2 + 0.5, 0, 0, 0, 0, 0);
                    }

                    // Linie entlang Z bei x1
                    for (int z = z1 + 1; z < z2; z++) {
                        viewer.spawnParticle(particle, x1 + 0.5, y + 0.1, z + 0.5, 0, 0, 0, 0, 0);
                    }

                    // Linie entlang Z bei x2
                    for (int z = z1 + 1; z < z2; z++) {
                        viewer.spawnParticle(particle, x2 + 0.5, y + 0.1, z + 0.5, 0, 0, 0, 0, 0);
                    }
                }


                if (ticksPassed % 20 == 0) {
                    int secondsLeft = (totalTicks - ticksPassed) / 20;
                    viewer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(IslandVault.getLanguageManager().translate(viewer, "radiusActionbarTimer", Map.of("secondsLeft", String.valueOf(secondsLeft)))));
                }
                ticksPassed += 2;

            }
        };
        activeTimers.put(viewer.getUniqueId(), task);
        task.runTaskTimer(IslandVault.getPlugin(), 0L, 2L);
    }

    public static boolean isTimerActive(Player player) {
        return activeTimers.containsKey(player.getUniqueId());
    }

    public static void stopTimer(Player player) {
        BukkitRunnable task = activeTimers.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
}
