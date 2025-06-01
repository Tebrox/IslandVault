package de.tebrox.islandVault.Commands.AdminCommand;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.CommandManager.argumentMatcher.ContainingAllCharsOfStringArgumentMatcher;
import de.tebrox.islandVault.Utils.PermissionUtils;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.logging.Level;

public class VaultAdminMainCommand extends MainCommand {

    public VaultAdminMainCommand() {
        super("§cYou don't have permission to do that.", new ContainingAllCharsOfStringArgumentMatcher(), "help");
        PermissionUtils.registerPermission(Permissions.ADMIN_MAIN.getLabel(), "admin main permission", PermissionDefault.FALSE);
    }

    @Override
    protected void registerSubCommands() {
        //subCommands.add(new AdminHelpCommand());
        subCommands.add(new ReloadConfigCommand());
        subCommands.add(new ReloadLanguageCommand());
        subCommands.add(new SetDebugModeCommand());
        subCommands.add(new OpenPlayerVaultCommand());


        for(SubCommand cmd : getSubCommands()) {
            PermissionDefault permissionDefault = cmd.getPermissionDefault();
            if(permissionDefault == null) {
                permissionDefault = PermissionDefault.FALSE;
            }
            if(PermissionUtils.registerPermission(cmd.getPermission(), cmd.getDescription(), permissionDefault)) {
                IslandVault.getPlugin().getVaultLogger().log(Level.INFO, "Registered command: " + cmd.getSyntax());
            }else{
                IslandVault.getPlugin().getVaultLogger().warning("Cannot register command: " + cmd.getSyntax());
            }
        }
    }
}
