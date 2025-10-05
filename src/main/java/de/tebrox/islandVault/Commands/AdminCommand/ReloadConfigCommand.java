package de.tebrox.islandVault.Commands.AdminCommand;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import world.bentobox.bentobox.database.objects.Island;

import java.util.List;

public class ReloadConfigCommand implements SubCommand {
    @Override
    public String getName() {
        return "reloadConfig";
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin config file";
    }

    @Override
    public String getSyntax() {
        return "/insellageradmin reloadConfig";
    }

    @Override
    public String getPermission() {
        return Permissions.ADMIN_RELOAD_CONFIG.getLabel();
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return null;
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        if(index == 0) {
            return List.of("all", "itemGroups");
        }
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if(args[0].equalsIgnoreCase("all")) {
            sender.sendMessage("§aAlle Konfigurationen neu geladen.");
            IslandVault.getConfigManager().reloadAll();
        }else{
            String configName = args[0];
            if(IslandVault.getConfigManager().isConfigAvailable(configName)) {
                IslandVault.getConfigManager().reloadConfig(configName);
                sender.sendMessage("§aKonfiguration §d" + configName + " §aneu geladen.");
            }
        }
    }
}
