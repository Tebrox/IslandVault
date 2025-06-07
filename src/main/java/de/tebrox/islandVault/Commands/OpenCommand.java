package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.Enums.Commands;
import de.tebrox.islandVault.Enums.Data;
import de.tebrox.islandVault.Enums.Messages;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.MenuManager;
import de.tebrox.islandVault.Menu.VaultMenu;
import de.tebrox.islandVault.Utils.IslandUtils;
import me.kodysimpson.simpapi.colors.ColorTranslator;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

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
        return Commands.MAIN_COMMAND.getLabel() + " open";
    }

    @Override
    public String getPermission()
    {
        return Permissions.OPEN_GUI.getLabel();
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return null;
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        return null;
    }

    @Override
    public void perform(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player player)) {
            String message = "§cCan only use by players!";
            commandSender.sendMessage(message);
            return;
        }
        if(player.hasPermission(getPermission())) {
            IslandsManager islandsManager = IslandUtils.getIslandManager();
            if(islandsManager == null) {
                return;
            }
            Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);

            if(island == null) {
                player.sendMessage(IslandVault.getLanguageManager().translate(player, "notOnIsland"));
                return;
            }
            if(IslandUtils.hasAccessToCurrentIsland(player)) {
                try {
                    MenuManager.openMenu(VaultMenu.class, player);
                } catch (MenuManagerException | MenuManagerNotSetupException e) {
                    throw new RuntimeException(e);
                }
            }else{
                player.sendMessage(IslandVault.getLanguageManager().translate(player, "notTeamMember"));
            }
        }else{
            player.sendMessage(IslandVault.getLanguageManager().translate(player, "noPermission"));
        }
    }
}
