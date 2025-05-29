package de.tebrox.islandVault.Utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Material;

import java.util.UUID;

public class LuckPermsUtils {
    public static boolean hasPermissionForItem(UUID uuid, Material material) {
        String perm = "islandvault.vault." + material.name().toLowerCase();
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
}
