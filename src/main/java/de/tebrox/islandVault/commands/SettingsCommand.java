package de.tebrox.islandVault.commands;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.menu.VaultMenu;
import me.kodysimpson.simpapi.command.SubCommand;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SettingsCommand extends SubCommand {
    @Override
    public String getName() {
        return "settings";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "open the island vault settings";
    }

    @Override
    public String getSyntax() {
        return "/islandvault settings";
    }

    @Override
    public void perform(CommandSender sender, String[] strings) {
        if(!(sender instanceof Player player)) {
            String message = IslandVault.getPlugin().getConfig().getString("messages.noPlayer");
            IslandVault.getPlugin().getServer().sendPlainMessage(message);
            return;
        }

        try {
            MenuManager.openMenu(VaultMenu.class, (Player) player);
        } catch (MenuManagerException | MenuManagerNotSetupException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
