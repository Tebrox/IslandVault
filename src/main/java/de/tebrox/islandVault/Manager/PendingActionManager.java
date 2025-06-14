package de.tebrox.islandVault.Manager;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PendingActionManager {

    private static final Map<UUID, Runnable> confirmActions = new HashMap<>();
    private static final Map<UUID, Runnable> cancelActions = new HashMap<>();

    public static void registerPendingAction(UUID uuid, Runnable confirmAction) {
        confirmActions.put(uuid, confirmAction);
    }

    public static void registerCancelAction(UUID uuid, Runnable cancelAction) {
        cancelActions.put(uuid, cancelAction);
    }

    public static boolean hasPendingAction(UUID uuid) {
        return confirmActions.containsKey(uuid);
    }

    public static boolean hasCancelAction(UUID uuid) {
        return cancelActions.containsKey(uuid);
    }

    public static Runnable getPendingAction(UUID uuid) {
        return confirmActions.get(uuid);
    }

    public static Runnable getCancelAction(UUID uuid) {
        return cancelActions.get(uuid);
    }

    public static void clearPendingActions(UUID uuid) {
        confirmActions.remove(uuid);
        cancelActions.remove(uuid);
    }
}