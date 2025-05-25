package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.Enums.Commands;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpCommand implements SubCommand {


    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Shows the help of this plugin";
    }

    @Override
    public String getSyntax() {
        return Commands.MAIN_COMMAND + " help";
    }

    @Override
    public String getPermission() {
        return Permissions.COMMAND.getLabel() + getName();
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
                sender.sendMessage(subCommand.getSyntax() + " - " + subCommand.getDescription() + " (" + subCommand.getPermission() + ")");
        }
    }
}
