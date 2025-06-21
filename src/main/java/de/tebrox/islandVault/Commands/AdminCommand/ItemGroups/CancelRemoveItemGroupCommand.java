package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.List;

public class CancelRemoveItemGroupCommand implements SubCommand {

    @Override
    public String getName() {
        return "cancelremove";
    }

    @Override
    public String getDescription() {
        return "Bricht das Entfernen der Gruppe ab";
    }

    @Override
    public String getSyntax() {
        return "/vaultadmin cancelremove";
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
    public boolean isHidden() {
        return true;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        // Keine TabCompletion, da versteckt
        return Collections.emptyList();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        sender.sendMessage("Entfernen abgebrochen.");
    }
}