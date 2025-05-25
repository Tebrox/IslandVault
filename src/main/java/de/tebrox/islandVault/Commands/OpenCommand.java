package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.Enums.Commands;
import de.tebrox.islandVault.Enums.Messages;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Menu.VaultMenu;
import me.kodysimpson.simpapi.colors.ColorTranslator;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class OpenCommand implements SubCommand {


    @Override
    public String getName()
    {
        return "open";
    }

    @Override
    public String getDescription()
    {
        return "Opens the islandvault gui";
    }

    @Override
    public String getSyntax()
    {
        return Commands.MAIN_COMMAND + " open";
    }

    @Override
    public String getPermission()
    {
        return Permissions.COMMAND.getLabel() + getName() + "GUI";
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        return null;
    }

    @Override
    public void perform(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player player)) {
            String message = IslandVault.getPlugin().getConfig().getString(Messages.NO_PLAYER.getLabel());
            commandSender.sendMessage(message);
            return;
        }

        if(player.hasPermission(Permissions.CAN_OPEN_MENU.getLabel())) {
            try {
                MenuManager.openMenu(VaultMenu.class, player);
            } catch (MenuManagerException e) {
                throw new RuntimeException(e);
            } catch (MenuManagerNotSetupException e) {
                throw new RuntimeException(e);
            }
        }else{
            player.sendMessage(ColorTranslator.translateColorCodes(IslandVault.getPlugin().getConfig().getString(Messages.NO_PERMISSION.getLabel())));
        }
    }
}
