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
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

import java.awt.*;
import java.util.List;

public class BackbackCommand implements SubCommand {


    @Override
    public String getName()
    {
        return "backpack";
    }

    @Override
    public String getDescription()
    {
        return "Opens the islandvault backpack gui";
    }

    @Override
    public String getSyntax()
    {
        return Commands.MAIN_COMMAND.getLabel() + " backpack";
    }

    @Override
    public String getPermission()
    {
        return Permissions.USE_BACKPACK.getLabel();
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
                if(!IslandVault.getVaultManager().getVaults().containsKey(island.getOwner())) {
                    IslandVault.getVaultManager().loadVault(island.getOwner());
                    player.sendMessage(Color.RED + "Das Lager wird derzeit geladen, bitte versuche es gleich erneut!");
                    return;
                }
                IslandVault.getVaultManager().getVaults().get(island.getOwner()).openBackpack(player);
                player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1, 1);
            }else{
                player.sendMessage(IslandVault.getLanguageManager().translate(player, "notTeamMember"));
            }
        }else{
            player.sendMessage(IslandVault.getLanguageManager().translate(player, "noPermission"));
        }
    }
}
