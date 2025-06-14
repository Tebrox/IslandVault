package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

public class PermissionUtils {
    /**
     * Registriert eine Permission nur, wenn sie noch nicht existiert.
     *
     * @param permissionName Name der Permission (z. B. "meinplugin.meinbefehl")
     * @param description    Beschreibung der Permission
     * @param defaultPerm    Standardwert (PermissionDefault.OP, TRUE, FALSE, etc.)
     * @return true, wenn die Permission neu registriert wurde; false wenn schon vorhanden
     */
    public static boolean registerPermission(String permissionName, String description, PermissionDefault defaultPerm) {
        if(permissionName != null) {
            if (Bukkit.getPluginManager().getPermission(permissionName) == null) {
                Permission permission = new Permission(permissionName, description, defaultPerm);
                Bukkit.getPluginManager().addPermission(permission);
                return true;
            }
        }

        return false;
    }
}
