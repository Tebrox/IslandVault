package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ListGroupsCommand implements SubCommand {

    @Override
    public String getName() {
        return "listgroups";
    }

    @Override
    public String getDescription() {
        return "Zeigt alle verfügbaren Itemgruppen an.";
    }

    @Override
    public String getSyntax() {
        return "/vaultadmin listgroups";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.OP;
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        Set<String> groups = ItemGroupManager.getPermissionGroups().keySet();

        if (groups.isEmpty()) {
            sender.sendMessage("§cEs sind keine Itemgruppen vorhanden.");
        } else {
            sender.sendMessage("§7Vorhandene Gruppen:");
            for (String group : groups) {
                sender.sendMessage(" §8- §e" + group);
            }
        }
    }
}