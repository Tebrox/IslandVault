package de.tebrox.islandVault.Manager;

import world.bentobox.bentobox.database.objects.Island;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IslandTracker {
    private static final Map<UUID, Island> playerIslands = new HashMap<>();
    private static final Map<String, Integer> islandPlayerCounts = new HashMap<>();

    public static void setPlayerIsland(UUID playerUUID, Island island) {
        Island previous = playerIslands.put(playerUUID, island);

        if (previous != null && !previous.equals(island)) {
            decrementIslandCount(previous.getUniqueId());
        }

        incrementIslandCount(island.getUniqueId());
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
    }

    private static void incrementIslandCount(String islandUUID) {
        islandPlayerCounts.merge(islandUUID, 1, Integer::sum);
    }

    private static void decrementIslandCount(String islandUUID) {
        islandPlayerCounts.computeIfPresent(islandUUID, (uuid, count) -> (count > 1) ? count - 1 : null);
    }
}
