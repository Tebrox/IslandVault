package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.*;
import java.util.stream.Collectors;

public class RemoveItemFromGroupCommand implements SubCommand {

    private final Map<String, List<String>> permissionGroups;

    public RemoveItemFromGroupCommand(Map<String, List<String>> permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    @Override
    public String getName() {
        return "removeitem";
    }

    @Override
    public String getDescription() {
        return "Removes items from a permission group";
    }

    @Override
    public String getSyntax() {
        return "/group removeitem <group> <items>";
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
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        if (index == 0) {
            // Gruppennamen vorschlagen
            String partialGroup = args.length > 0 ? args[0].toLowerCase() : "";
            return permissionGroups.keySet().stream()
                    .filter(g -> g.startsWith(partialGroup))
                    .collect(Collectors.toList());
        }

        if (index == 1) {
            if (args.length < 1) return Collections.emptyList();
            String groupName = args[0].toLowerCase();
            if (!permissionGroups.containsKey(groupName)) return Collections.emptyList();

            List<String> currentItems = permissionGroups.get(groupName);

            String typed = args.length > 1 ? args[1].toUpperCase() : "";
            String[] parts = typed.split(",");
            String lastPart = parts[parts.length - 1].trim();

            // Nur Items, die aktuell in der Gruppe sind und noch nicht in der Liste stehen
            List<String> completions = new ArrayList<>();
            for (String item : currentItems) {
                if (item.startsWith(lastPart) && !Arrays.asList(parts).contains(item)) {
                    String prefix = parts.length > 1 ? String.join(",", Arrays.copyOf(parts, parts.length - 1)) + "," : "";
                    completions.add(prefix + item);
                }
            }
            return completions;
        }

        return Collections.emptyList();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: " + getSyntax());
            return;
        }

        String groupName = args[0].toLowerCase();
        if (!permissionGroups.containsKey(groupName)) {
            sender.sendMessage("Group '" + groupName + "' does not exist.");
            return;
        }

        String[] itemsToRemove = args[1].toUpperCase().split(",");
        List<String> groupItems = permissionGroups.get(groupName);

        int removedCount = 0;
        for (String item : itemsToRemove) {
            if (groupItems.contains(item)) {
                groupItems.remove(item);
                removedCount++;
            }
        }
        ItemGroupManager.saveGroup(groupName, groupItems);
        sender.sendMessage("Removed " + removedCount + " item(s) from group '" + groupName + "'.");
    }
}