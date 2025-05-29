package de.tebrox.islandVault.Manager;

import java.util.*;

public class IslandPresenceTracker {

    private static final Map<String, Set<UUID>> islandPlayerMap = new HashMap<>();

    public static void playerEnteredIsland(String islandUUID, UUID playerUUID) {
        islandPlayerMap.computeIfAbsent(islandUUID, k -> new HashSet<>()).add(playerUUID);
    }

    public static void playerLeftIsland(String islandUUID, UUID playerUUID) {
        Set<UUID> players = islandPlayerMap.get(islandUUID);
        if (players != null) {
            players.remove(playerUUID);
            if (players.isEmpty()) {
                islandPlayerMap.remove(islandUUID);
            }
        }
    }

    public static Set<UUID> getPlayersOnIsland(String islandUUID) {
        return islandPlayerMap.getOrDefault(islandUUID, Collections.emptySet());
    }

    public static int getPlayerCount(String islandUUID) {
        return getPlayersOnIsland(islandUUID).size();
    }

    public static boolean isIslandIsEmpty(String islandUUID) {
        return getPlayersOnIsland(islandUUID).isEmpty();
    }
}