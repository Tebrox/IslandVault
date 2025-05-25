package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.Enums.Commands;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ItemSearchCommand implements SubCommand {
    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getDescription() {
        return "Open the islandvault with search syntax";
    }

    @Override
    public String getSyntax() {
        return Commands.MAIN_COMMAND.getLabel() + " search <item>";
    }

    @Override
    public String getPermission() {
        return "";
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        //TODO
    }
}
