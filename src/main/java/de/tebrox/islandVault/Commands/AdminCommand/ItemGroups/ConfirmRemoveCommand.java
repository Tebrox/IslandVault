package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.PendingActionManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ConfirmRemoveCommand implements SubCommand {

    @Override public String getName() { return "confirmremove"; }
    @Override public String getDescription() { return "Bestätigt Item-Entfernung"; }
    @Override public String getSyntax() { return "/insellageradmin confirmremove"; }
    @Override public String getPermission() { return null; }
    @Override public boolean isHidden() { return true; }
    @Override public PermissionDefault getPermissionDefault() { return PermissionDefault.OP; }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int i, String[] args) { return Collections.emptyList(); }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur ingame verwendet werden.");
            return;
        }

        UUID uuid = player.getUniqueId();

        if (!PendingActionManager.hasPendingAction(uuid)) {
            player.sendMessage("Du hast keine ausstehende Aktion zum Bestätigen.");
            return;
        }

        Runnable action = PendingActionManager.getPendingAction(uuid);
        if (action != null) {
            action.run();
        }
        PendingActionManager.clearPendingActions(uuid);
        player.sendMessage("Aktion bestätigt.");
    }
}
