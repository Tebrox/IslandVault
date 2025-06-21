package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.*;
import java.util.stream.Collectors;

public class AddItemToGroupCommand implements SubCommand {

    private final Map<String, List<String>> permissionGroups;

    public AddItemToGroupCommand(Map<String, List<String>> permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    @Override
    public String getName() {
        return "addGroupItem";
    }

    @Override
    public String getDescription() {
        return "Adds items to a permission group";
    }

    @Override
    public String getSyntax() {
        return "/group additem <group> <items>";
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
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        // index 0 = Gruppenname, index 1 = items
        if (index == 0) {
            String partialGroup = !args[1].isEmpty() ? args[1].toLowerCase() : "";
            return ItemGroupManager.getPermissionGroups().keySet().stream()
                    .filter(g -> g.startsWith(partialGroup))
                    .collect(Collectors.toList());
        }

        if (index == 1) {
            if (args[1].length() < 1) return Collections.emptyList();
            String groupName = args[1].toLowerCase();
            if (!ItemGroupManager.getPermissionGroups().containsKey(groupName)) return Collections.emptyList();

            List<String> currentItems = ItemGroupManager.getPermissionGroups().get(groupName);

            String typed = args[1].length() > 1 ? args[1].toUpperCase() : "";
            String[] parts = typed.split(",");
            String lastPart = parts[parts.length - 1].trim();

            List<String> materials = Arrays.stream(Material.values())
                    .map(Enum::name)
                    .filter(m -> !currentItems.contains(m)) // nur Items, die noch nicht drin sind
                    .collect(Collectors.toList());

            List<String> completions = new ArrayList<>();
            for (String mat : materials) {
                if (mat.startsWith(lastPart)) {
                    String prefix = parts.length > 1 ? String.join(",", Arrays.copyOf(parts, parts.length - 1)) + "," : "";
                    completions.add(prefix + mat);
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
            sender.sendMessage("Group does not exist.");
            return;
        }

        String[] itemsToAdd = args[1].toUpperCase().split(",");
        List<String> groupItems = permissionGroups.get(groupName);

        int addedCount = 0;
        for (String item : itemsToAdd) {
            if (!groupItems.contains(item) && isValidMaterial(item)) {
                groupItems.add(item);
                addedCount++;
            }
        }
        ItemGroupManager.saveGroup(groupName, groupItems);
        sender.sendMessage("Added " + addedCount + " items to group " + groupName + ".");
    }

    private boolean isValidMaterial(String name) {
        try {
            Material.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}