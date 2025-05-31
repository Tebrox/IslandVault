package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.Enums.Commands;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.MenuManager;
import de.tebrox.islandVault.Menu.VaultMenu;
import de.tebrox.islandVault.Utils.IslandUtils;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

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
        return Permissions.USE_SEARCH.getLabel();
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.TRUE;
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        return List.of();
    }

    @Override
    public void perform(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player player)) {
            String message = "§cCan only use by players!";
            commandSender.sendMessage(message);
            return;
        }

        if(args.length == 0) {
            commandSender.sendMessage("Bitte Suchbegriff eingeben!");
            return;
        }
        String searchQuery = args[0];

        if (player.hasPermission(getPermission())) {
            IslandsManager islandsManager = IslandUtils.getIslandManager();
            if (islandsManager == null) {
                return;
            }
            Island island = IslandUtils.getIslandManager().getIslandAt(player.getLocation()).orElse(null);

            if (island == null) {
                player.sendMessage(IslandVault.getLanguageManager().translate(player, "notOnIsland"));
                return;
            }
            if (IslandUtils.isOnIslandWhereMember(player) || IslandUtils.isOnOwnIsland(player)) {
                try {
                    MenuManager.getPlayerMenuUtility(player).setData("searchQuery", searchQuery);
                    MenuManager.openMenu(VaultMenu.class, player);
                } catch (MenuManagerException | MenuManagerNotSetupException e) {
                    throw new RuntimeException(e);
                }
            } else {
                player.sendMessage(IslandVault.getLanguageManager().translate(player, "notTeamMember"));
            }
        } else {
            player.sendMessage(IslandVault.getLanguageManager().translate(player, "noPermission"));
        }
    }
}
