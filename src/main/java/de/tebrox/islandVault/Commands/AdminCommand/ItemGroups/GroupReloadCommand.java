package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.List;

public class GroupReloadCommand implements SubCommand {

    @Override
    public String getName() {
        return "reloadGroups";
    }

    @Override
    public String getDescription() {
        return "Lädt die Gruppen neu";
    }

    @Override
    public String getSyntax() {
        return "/insellageradmin reloadGroups";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.TRUE;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        return List.of();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        ItemGroupManager.reloadGroups();
        sender.sendMessage("§aGruppen wurden neu geladen.");
    }
}