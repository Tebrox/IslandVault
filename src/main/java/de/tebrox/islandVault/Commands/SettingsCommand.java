package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.Enums.Commands;
import de.tebrox.islandVault.Enums.Messages;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Menu.SettingsMenu;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SettingsCommand implements SubCommand {
    @Override
    public String getName() {
        return "settings";
    }

    @Override
    public String getDescription() {
        return "open the island vault settings";
    }

    @Override
    public String getSyntax() {
        return Commands.MAIN_COMMAND + " settings";
    }

    @Override
    public String getPermission() {
        return "";
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        return List.of();
    }

    @Override
    public void perform(CommandSender sender, String[] strings) {
        if(!(sender instanceof Player player)) {
            String message = IslandVault.getPlugin().getConfig().getString(Messages.NO_PLAYER.getLabel());
            IslandVault.getPlugin().getServer().sendPlainMessage(message);
            return;
        }

        try {
            MenuManager.openMenu(SettingsMenu.class, player);
        } catch (MenuManagerException | MenuManagerNotSetupException e) {
            e.printStackTrace();
        }
    }

}
