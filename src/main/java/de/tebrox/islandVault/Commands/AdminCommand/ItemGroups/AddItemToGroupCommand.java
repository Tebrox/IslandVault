package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command implementation for adding items to a permission-based item group.
 * <p>
 * This command allows operators to add one or more {@link Material} names to a predefined permission group.
 * Group items are stored as uppercase material names.
 */
public class AddItemToGroupCommand implements SubCommand {
    private final Map<String, List<String>> permissionGroups;

    /**
     * Constructs the command with the given permission group storage.
     *
     * @param permissionGroups a map of group names to their respective item lists
     */
    public AddItemToGroupCommand(Map<String, List<String>> permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "addGroupItem";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Adds items to a permission group";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSyntax() {
        return "/group additem <group> <items>";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPermission() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.OP;
    }

    /**
     * Provides tab-completion suggestions for the group name and items.
     *
     * @param sender the command sender
     * @param index the argument index being typed
     * @param args the current command arguments
     * @return a list of possible completions
     */
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

    /**
     * Executes the command, adding the specified items to the target group.
     *
     * @param sender the command sender
     * @param args command arguments (expected: group name and comma-separated item list)
     */
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

    /**
     * Checks whether the given string is a valid {@link Material} name.
     *
     * @param name the material name to validate
     * @return true if the name matches a valid Material, false otherwise
     */
    private boolean isValidMaterial(String name) {
        try {
            Material.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}