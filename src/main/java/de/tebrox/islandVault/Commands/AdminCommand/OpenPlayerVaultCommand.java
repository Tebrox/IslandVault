package de.tebrox.islandVault.Commands.AdminCommand;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.MenuManager;
import de.tebrox.islandVault.Menu.VaultMenu;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpenPlayerVaultCommand implements SubCommand {
    @Override
    public String getName() {
        return "openPlayerVault";
    }

    @Override
    public String getDescription() {
        return "opens the specific players islandvault";
    }

    @Override
    public String getSyntax() {
        return "/insellageradmin openPlayerVault <PLAYER>";
    }

    @Override
    public String getPermission() {
        return Permissions.ADMIN_OPEN_PLAYER_VAULT.getLabel();
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return null;
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        if(index == 0) {
            List<String> players = new ArrayList<>();
            for(File file : IslandVault.getVaultManager().loadAllVaultsToList()) {
                players.add(Bukkit.getOfflinePlayer(UUID.fromString(file.getName().replaceAll(".yml", ""))).getName());
            }
            return players;
        }
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if(!(sender instanceof Player player)) {
            return;
        }
        if(args.length == 0) {
            return;
        }

        String owner = args[0];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);

        if(!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage("§c[IslandVaultAdmin] Dieser Spieler war noch nie auf dem Server!");
            return;
        }

        if(!IslandVault.getVaultManager().getVaults().containsKey(offlinePlayer.getUniqueId())) {
            IslandVault.getVaultManager().loadVault(offlinePlayer.getUniqueId());
        }


        try {
            MenuManager.getPlayerMenuUtility(player).setData("adminOpen", offlinePlayer.getUniqueId());
            MenuManager.openMenu(VaultMenu.class, player);
        } catch (MenuManagerException | MenuManagerNotSetupException e) {
            throw new RuntimeException(e);
        }
    }
}
