package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.Enums.Commands;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public class ShowAutoCollectMessageCommand implements SubCommand {
    @Override
    public String getName() {
        return "showAutocollectMessage";
    }

    @Override
    public String getDescription() {
        return "Shows the help of this plugin";
    }

    @Override
    public String getSyntax() {
        return "/insellager showAutocollectMessage <ON/OFF>";
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
    public List<String> getTabCompletion(int index, String[] args) {
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        sender.sendMessage("IslandVault version " + IslandVault.getPlugin().getDescription().getVersion());

        for (SubCommand subCommand : IslandVault.getMainCommand().getSubCommands())
        {
            if (sender.hasPermission(subCommand.getPermission()))
                sender.sendMessage(subCommand.getSyntax() + " - " + subCommand.getDescription());
        }
    }

}
