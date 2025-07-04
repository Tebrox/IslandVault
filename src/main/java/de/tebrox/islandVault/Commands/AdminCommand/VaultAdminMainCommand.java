package de.tebrox.islandVault.Commands.AdminCommand;

import de.tebrox.islandVault.Commands.AdminCommand.ItemGroups.*;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.CommandManager.argumentMatcher.ContainingAllCharsOfStringArgumentMatcher;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import de.tebrox.islandVault.Utils.PermissionUtils;
import de.tebrox.islandVault.Utils.PluginLogger;
import org.bukkit.permissions.PermissionDefault;

public class VaultAdminMainCommand extends MainCommand {

    public VaultAdminMainCommand() {
        super("§cYou don't have permission to do that.", new ContainingAllCharsOfStringArgumentMatcher(), "help");
        PermissionUtils.registerPermission(Permissions.ADMIN_MAIN.getLabel(), "admin main permission", PermissionDefault.FALSE);
    }

    @Override
    protected void registerSubCommands() {
        subCommands.add(new AdminHelpCommand());
        subCommands.add(new BlacklistCommand());

        subCommands.add(new ReloadConfigCommand());
        subCommands.add(new ReloadLanguageCommand());
        subCommands.add(new SetDebugModeCommand());
        subCommands.add(new AddItemToGroupCommand(ItemGroupManager.getPermissionGroups()));
        subCommands.add(new CancelRemoveCommand());
        subCommands.add(new CancelRemoveItemGroupCommand());
        subCommands.add(new ConfirmRemoveCommand());
        subCommands.add(new ConfirmRemoveItemGroupCommand());
        subCommands.add(new CreateGroupCommand(ItemGroupManager.getPermissionGroups()));
        subCommands.add(new DeleteGroupCommand(ItemGroupManager.getPermissionGroups()));
        //subCommands.add(new EditGroupChatCommand());
        subCommands.add(new GroupReloadCommand());
        subCommands.add(new ListGroupsCommand());
        subCommands.add(new RemoveItemFromSessionCommand());
        subCommands.add(new RemoveItemFromGroupCommand(ItemGroupManager.getPermissionGroups()));


        for(SubCommand cmd : getSubCommands()) {
            PermissionDefault permissionDefault = cmd.getPermissionDefault();
            if(permissionDefault == null) {
                permissionDefault = PermissionDefault.FALSE;
            }

            if(cmd.getPermission() == null) continue;

            if(PermissionUtils.registerPermission(cmd.getPermission(), cmd.getDescription(), permissionDefault)) {
                PluginLogger.info("Registered command: " + cmd.getSyntax());
            }else{
                PluginLogger.warning("Cannot register command: " + cmd.getSyntax());
            }
        }
    }
}
