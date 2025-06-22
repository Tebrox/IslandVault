package de.tebrox.islandVault.Manager;

import world.bentobox.bentobox.database.objects.Island;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class to track online players on islands.
 * <p>
 * Keeps a record of which player is currently on which island and
 * how many players are currently online on each island.
 * </p>
 */
public class IslandTracker {
    /** Maps player UUIDs to their current island. */
    private static final Map<UUID, Island> playerIslands = new HashMap<>();

    /** Maps island UUIDs to the number of currently online players. */
    private static final Map<String, Integer> islandPlayerCounts = new HashMap<>();


    /**
     * Associates a player with an island and updates the online player count accordingly.
     *
     * @param playerUUID the UUID of the player
     * @param island     the island the player is now on
     */
    public static void setPlayerIsland(UUID playerUUID, Island island) {
        Island previous = playerIslands.put(playerUUID, island);

        if (previous != null && !previous.equals(island)) {
            decrementIslandCount(previous.getUniqueId());
        }

        incrementIslandCount(island.getUniqueId());
    }

    /**
     * Removes a player from tracking and decreases the island's player count.
     *
     * @param playerUUID the UUID of the player to remove
     */
    public static void removePlayerIsland(UUID playerUUID) {
        Island island = playerIslands.remove(playerUUID);
        if (island != null) {
            decrementIslandCount(island.getUniqueId());
        }
    }

    /**
     * Gets the island a player is currently on.
     *
     * @param playerUUID the UUID of the player
     * @return the island the player is on, or null if not tracked
     */
    public static Island getPlayerIsland(UUID playerUUID) {
        return playerIslands.get(playerUUID);
    }

    /**
     * Checks if an island currently has no players online.
     *
     * @param islandUUID the UUID of the island
     * @return true if no players are on the island; false otherwise
     */
    public static boolean isIslandEmpty(String islandUUID) {
        return getOnlinePlayerCount(islandUUID) == 0;
    }

    /**
     * Gets the number of players currently online on a given island.
     *
     * @param islandUUID the UUID of the island
     * @return the number of players currently on the island
     */
    public static int getOnlinePlayerCount(String islandUUID) {
        return islandPlayerCounts.getOrDefault(islandUUID, 0);
    }

    /**
     * Clears all tracked player-island associations and resets all player counts.
     */
    public static void reset() {
        playerIslands.clear();
        islandPlayerCounts.clear();
    }

    /**
     * Internally increments the player count for a given island
     */
    private static void incrementIslandCount(String islandUUID) {
        islandPlayerCounts.merge(islandUUID, 1, Integer::sum);
    }

    /**
     * Internally decrements the player count for a given island, or removes it if the count reaches 0
     */

    private static void decrementIslandCount(String islandUUID) {
        islandPlayerCounts.computeIfPresent(islandUUID, (uuid, count) -> (count > 1) ? count - 1 : null);
    }
}
