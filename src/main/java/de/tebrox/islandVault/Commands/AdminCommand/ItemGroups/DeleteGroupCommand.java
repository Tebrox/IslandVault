package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeleteGroupCommand implements SubCommand {

    private final Map<String, List<String>> permissionGroups;

    public DeleteGroupCommand(Map<String, List<String>> permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Deletes a permission group";
    }

    @Override
    public String getSyntax() {
        return "/group delete <group>";
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
        System.out.println(args[1].toString());
        System.out.println("index: " + index);
        if (index == 0) {
            String partialGroup = !args[0].isEmpty() ? args[1].toLowerCase() : "";
            return ItemGroupManager.getPermissionGroups().keySet().stream()
                    .filter(g -> g.startsWith(partialGroup))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: " + getSyntax());
            return;
        }

        String groupName = args[0].toLowerCase();
        if (!permissionGroups.containsKey(groupName)) {
            sender.sendMessage("Group does not exist.");
            return;
        }

        ItemGroupManager.deleteGroup(groupName);
        sender.sendMessage("Group " + groupName + " deleted.");
    }
}
