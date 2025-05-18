package de.tebrox.islandVault.commands;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.menu.VaultMenu;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class OpenCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player player)) {
            String message = IslandVault.getPlugin().getConfig().getString("messages.noPlayer");
            commandSender.sendMessage(message);
            return true;
        }

        try {
            MenuManager.openMenu(VaultMenu.class, player);
        } catch (MenuManagerException e) {
            throw new RuntimeException(e);
        } catch (MenuManagerNotSetupException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
