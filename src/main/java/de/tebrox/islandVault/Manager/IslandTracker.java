package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Utils.PlayerVaultUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import world.bentobox.bentobox.database.objects.Island;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class IslandTracker {
    private static final Map<UUID, Island> playerIslands = new HashMap<>();
    private static final Map<String, Integer> islandPlayerCounts = new HashMap<>();

    private static final Map<String, BukkitTask> islandRemovalTasks = new HashMap<>();
    private static final long EXPIRE_TICKS = 20L * 60 * 5; // 5 Minuten

    public static void setPlayerIsland(UUID playerUUID, Island island) {
        Island previous = playerIslands.put(playerUUID, island);

        if (previous != null && !previous.equals(island)) {
            decrementIslandCount(previous.getUniqueId());
        }

        incrementIslandCount(island.getUniqueId());
        if (island != null) {
            UUID ownerUUID = island.getOwner();
            LuckPerms lp = LuckPermsProvider.get();

            if(!IslandVault.getVaultManager().getVaults().containsKey(ownerUUID)) {
                IslandVault.getVaultManager().getPlayerVaultItems(Bukkit.getPlayer(playerUUID), island.getOwner());
            }

            if (lp.getUserManager().getUser(ownerUUID) == null) {
                lp.getUserManager().loadUser(ownerUUID);
                //System.out.println("Load user in luckperms cache: " + Bukkit.getOfflinePlayer(ownerUUID).getName());
            }
        }
    }

    public static void removePlayerIsland(UUID playerUUID) {
        Island island = playerIslands.remove(playerUUID);
        if (island != null) {
            decrementIslandCount(island.getUniqueId());
        }
    }

    public static Island getPlayerIsland(UUID playerUUID) {
        return playerIslands.get(playerUUID);
    }

    public static boolean isIslandEmpty(String islandUUID) {
        return getOnlinePlayerCount(islandUUID) == 0;
    }

    public static int getOnlinePlayerCount(String islandUUID) {
        return islandPlayerCounts.getOrDefault(islandUUID, 0);
    }

    public static void reset() {
        playerIslands.clear();
        islandPlayerCounts.clear();

        islandRemovalTasks.values().forEach(BukkitTask::cancel);
        islandRemovalTasks.clear();
    }

    private static void incrementIslandCount(String islandUUID) {
        islandPlayerCounts.merge(islandUUID, 1, Integer::sum);
        cancelExpire(islandUUID);
    }

    private static void decrementIslandCount(String islandUUID) {
        islandPlayerCounts.computeIfPresent(islandUUID, (uuid, count) -> (count > 1) ? count - 1 : null);
        /**islandPlayerCounts.computeIfPresent(islandUUID, (uuid, count) -> {
            int newCount = count - 1;
            if (newCount <= 0) {
                scheduleExpire(islandUUID);
                return null; // Zähler entfernen
            }
            return newCount;
        });**/

        if(isIslandEmpty(islandUUID)) {
            scheduleExpire(islandUUID);
        }
    }

    private static void scheduleExpire(String islandUUID) {
        if (islandRemovalTasks.containsKey(islandUUID)) return; // schon eingeplant
        //System.out.println("Insel " + islandUUID + " ist in der Entladeliste!");
        BukkitTask task = Bukkit.getScheduler().runTaskLater(IslandVault.getPlugin(), () -> {
            if (isIslandEmpty(islandUUID)) {
                unloadIsland(islandUUID);
            }
        }, EXPIRE_TICKS);

        islandRemovalTasks.put(islandUUID, task);
    }

    private static void cancelExpire(String islandUUID) {
        BukkitTask task = islandRemovalTasks.remove(islandUUID);
        if (task != null) {
            task.cancel();
        }
    }

    private static void unloadIsland(String islandUUID) {
        // Spielerzuordnungen entfernen
        playerIslands.entrySet().removeIf(entry -> {
            Island island = entry.getValue();
            UUID owner = island.getOwner();
            if (IslandVault.getVaultManager().getVaults().containsKey(owner)) {
                PlayerVaultUtils playerVaultUtils = IslandVault.getVaultManager().getVaults().get(owner);
                IslandVault.getVaultManager().unloadVault(playerVaultUtils, owner);
            }

            return island != null && island.getUniqueId().equals(islandUUID);
        });

        // Task entfernen
        islandRemovalTasks.remove(islandUUID);

        // Hier Persistenz oder sonstiges Unload-Handling einfügen
        //System.out.println("Insel " + islandUUID + " wurde entladen und aus playerIslands entfernt.");
    }
}
