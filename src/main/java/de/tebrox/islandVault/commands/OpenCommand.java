package de.tebrox.islandVault.commands;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.enums.Messages;
import de.tebrox.islandVault.enums.Permissions;
import de.tebrox.islandVault.menu.VaultMenu;
import me.kodysimpson.simpapi.colors.ColorTranslator;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class OpenCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player player)) {
            String message = IslandVault.getPlugin().getConfig().getString(Messages.NO_PLAYER.getLabel());
            commandSender.sendMessage(message);
            return true;
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


        return true;
    }
}
