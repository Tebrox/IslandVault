package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.List;

/**
 * Command to cancel the removal of an item group.
 * <p>
 * This command is hidden and does not provide any tab completion options.
 * When executed, it simply notifies the sender that the removal has been cancelled.
 */
public class CancelRemoveItemGroupCommand implements SubCommand {

    /**
     * Gets the name of this command.
     *
     * @return the command name ("cancelremove")
     */
    @Override
    public String getName() {
        return "cancelremove";
    }

    /**
     * Gets a short description of this command.
     *
     * @return a description string
     */
    @Override
    public String getDescription() {
        return "Bricht das Entfernen der Gruppe ab";
    }

    /**
     * Gets the syntax for using this command.
     *
     * @return the command syntax string
     */
    @Override
    public String getSyntax() {
        return "/vaultadmin cancelremove";
    }

    /**
     * Gets the permission node required to execute this command.
     *
     * @return null since no permission node is required
     */
    @Override
    public String getPermission() {
        return null;
    }

    /**
     * Gets the default permission level required to execute this command.
     *
     * @return {@link PermissionDefault#FALSE} indicating no permission by default
     */
    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.FALSE;
    }

    /**
     * Determines if this command should be hidden from command lists.
     *
     * @return true to hide the command
     */
    @Override
    public boolean isHidden() {
        return true;
    }

    /**
     * Provides tab completion options for the command.
     * Since the command is hidden and takes no arguments, this returns an empty list.
     *
     * @param sender the command sender
     * @param index the argument index
     * @param args the command arguments
     * @return an empty list of completions
     */
    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        // Keine TabCompletion, da versteckt
        return Collections.emptyList();
    }

    /**
     * Executes the command.
     * Sends a confirmation message to the sender that the removal has been cancelled.
     *
     * @param sender the command sender
     * @param args the command arguments
     */
    @Override
    public void perform(CommandSender sender, String[] args) {
        sender.sendMessage("Entfernen abgebrochen.");
    }
}