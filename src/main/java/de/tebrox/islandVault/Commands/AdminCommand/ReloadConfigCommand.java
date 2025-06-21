package de.tebrox.islandVault.Commands.AdminCommand;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public class ReloadConfigCommand implements SubCommand {
    @Override
    public String getName() {
        return "reloadConfig";
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin config file";
    }

    @Override
    public String getSyntax() {
        return "/insellageradmin reloadConfig";
    }

    @Override
    public String getPermission() {
        return Permissions.ADMIN_RELOAD_CONFIG.getLabel();
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return null;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        return List.of();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        IslandVault.getPlugin().reloadPluginConfig(sender);
    }
}
