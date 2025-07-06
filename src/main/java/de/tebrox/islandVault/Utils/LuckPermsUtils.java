package de.tebrox.islandVault.Utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Material;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsUtils {
    public static boolean hasPermissionForItem(UUID uuid, String itemID) {
        String perm = "islandvault.vault." + itemID;
        return checkPermission(uuid, perm);
    }

    public static boolean hasPermissionForGroup(UUID uuid, String group) {
        String perm = "islandvault.groups." + group;
        return checkPermission(uuid, perm);
    }

    public static boolean hasPermissionForCommand(UUID uuid, String cmd) {
        String perm = "islandvault.command." + cmd;
        return checkPermission(uuid, perm);
    }

    public static int getMaxRadiusFromPermissions(UUID uuid) {
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(uuid);
        if (user == null) return 0;

        int max = 0;
        for (Node node : user.getNodes()) {
            if (!node.getKey().startsWith("islandvault.collectradius.")) continue;
            try {
                int value = Integer.parseInt(node.getKey().substring("islandvault.collectradius.".length()));
                max = Math.max(max, value);
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }

    private static boolean checkPermission(UUID uuid, String permission) {
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(uuid);

        if(user == null) return false;

        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    /**
     * Prüft asynchron, ob der User die Item-Permission besitzt.
     * Lädt ggf. den LuckPerms-User.
     */
    public static CompletableFuture<Boolean> hasPermissionForItemAsync(UUID uuid, String item) {
        String perm = "islandvault.vault." + item.toLowerCase(Locale.ROOT);
        return checkPermissionAsync(uuid, perm);
    }

    /**
     * Prüft asynchron, ob der User die Gruppen-Permission besitzt.
     */
    public static CompletableFuture<Boolean> hasPermissionForGroupAsync(UUID uuid, String group) {
        String perm = "islandvault.groups." + group;
        return checkPermissionAsync(uuid, perm);
    }

    private static CompletableFuture<Boolean> checkPermissionAsync(UUID uuid, String permission) {
        LuckPerms lp = LuckPermsProvider.get();
        return lp.getUserManager().loadUser(uuid).thenApplyAsync(user -> {
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        });
    }
}
