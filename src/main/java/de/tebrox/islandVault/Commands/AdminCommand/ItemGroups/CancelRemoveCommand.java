package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.PendingActionManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A hidden admin subcommand that allows a player to cancel a pending item removal action.
 * <p>
 * This command is intended for internal use only and should not be listed in public command help menus.
 */
public class CancelRemoveCommand implements SubCommand {

    /**
     * {@inheritDoc}
     */
    @Override public String getName() { return "cancelremove"; }

    /**
     * {@inheritDoc}
     */
    @Override public String getDescription() { return "Abbruch einer Item-Entfernung"; }

    /**
     * {@inheritDoc}
     */
    @Override public String getSyntax() { return "/insellageradmin cancelremove"; }
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

        if (!PendingActionManager.hasCancelAction(uuid)) {
            player.sendMessage("Du hast keine ausstehende Aktion zum Abbrechen.");
            return;
        }

        Runnable cancelAction = PendingActionManager.getCancelAction(uuid);
        if (cancelAction != null) {
            cancelAction.run();
        }
        PendingActionManager.clearPendingActions(uuid);
        player.sendMessage("Aktion abgebrochen.");
    }
}