package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import de.tebrox.islandVault.Utils.PermissionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateGroupCommand implements SubCommand {

    private final Map<String, List<String>> permissionGroups;

    public CreateGroupCommand(Map<String, List<String>> permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    @Override
    public String getName() {
        return "createGroup";
    }

    @Override
    public String getDescription() {
        return "Creates a new permission group";
    }

    @Override
    public String getSyntax() {
        return "/insellageradmin createGroup <group>";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.FALSE;
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        // Keine TabCompletion für Gruppenname beim Erstellen
        return List.of();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: " + getSyntax());
            return;
        }

        String groupName = args[0].toLowerCase();

        if (permissionGroups.containsKey(groupName)) {
            sender.sendMessage("Group '" + groupName + "' already exists.");
            return;
        }

        ItemGroupManager.saveGroup(groupName, new ArrayList<>());
        PermissionUtils.registerPermission(Permissions.GROUPS + groupName, "Group " + groupName, PermissionDefault.FALSE);
        sender.sendMessage("Group '" + groupName + "' created.");
    }
}