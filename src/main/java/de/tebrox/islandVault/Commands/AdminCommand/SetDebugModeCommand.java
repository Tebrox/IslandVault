package de.tebrox.islandVault.Commands.AdminCommand;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.List;

public class SetDebugModeCommand implements SubCommand {
    @Override
    public String getName() {
        return "setDebugMode";
    }

    @Override
    public String getDescription() {
        return "sets the debugmode to activ or inactive";
    }

    @Override
    public String getSyntax() {
        return "/insellageradmin setDebugMode <ON/OFF>";
    }

    @Override
    public String getPermission() {
        return Permissions.ADMIN_SET_DEBUGMODE.getLabel();
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return null;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        if(index == 0) {
            return List.of("on", "off");
        }
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if(args[0].equalsIgnoreCase("on")) {
            sender.sendMessage("§aDebugmodus aktiviert.");
            IslandVault.getPlugin().getLogger().info("Debugmodus wurde aktiviert.");
            IslandVault.getPlugin().setDebugMode(true);

        }else if(args[0].equalsIgnoreCase("off")) {
            sender.sendMessage("§aDebugmodus deaktiviert.");
            IslandVault.getPlugin().getLogger().info("Debugmodus wurde deaktiviert.");
            IslandVault.getPlugin().setDebugMode(false);
        }
    }
}
