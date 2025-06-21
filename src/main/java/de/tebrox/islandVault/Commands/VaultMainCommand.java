package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.MainCommand;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.CommandManager.argumentMatcher.ContainingAllCharsOfStringArgumentMatcher;
import de.tebrox.islandVault.Utils.PermissionUtils;
import org.bukkit.permissions.PermissionDefault;

import java.util.logging.Level;

public class VaultMainCommand extends MainCommand {

    public VaultMainCommand() {
        super("§cYou don't have permission to do that.", new ContainingAllCharsOfStringArgumentMatcher(), "open");
    }

    @Override
    protected void registerSubCommands() {
        subCommands.add(new OpenCommand());
        subCommands.add(new HelpCommand());
        subCommands.add(new ItemSearchCommand());
        subCommands.add(new SettingsCommand());
        subCommands.add(new RegionCommand());


        for(SubCommand cmd : getSubCommands()) {
            PermissionDefault permissionDefault = cmd.getPermissionDefault();
            if(permissionDefault == null) {
                permissionDefault = PermissionDefault.FALSE;
            }

            if(cmd.getPermission() == null || cmd.getPermission().isEmpty()) continue;

            if(PermissionUtils.registerPermission(cmd.getPermission(), cmd.getDescription(), permissionDefault)) {
                IslandVault.getPlugin().getLogger().log(Level.INFO, "Registered permission: " + cmd.getSyntax());
            }else{
                IslandVault.getPlugin().getLogger().warning("Cannot register permission: " + cmd.getSyntax());
            }

        }

    }
}
