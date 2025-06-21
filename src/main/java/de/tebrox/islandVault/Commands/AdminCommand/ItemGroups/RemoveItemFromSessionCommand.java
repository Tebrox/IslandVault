package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Utils.ConfirmationPrompt;
import de.tebrox.islandVault.Utils.GroupEditSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RemoveItemFromSessionCommand implements SubCommand {

    @Override public String getName() { return "removeitemfromsession"; }
    @Override public String getDescription() { return "Löschen eines Items aus der Session"; }
    @Override public String getSyntax() { return "/vaultadmin removeitemfromsession <item>"; }
    @Override public String getPermission() { return null; }
    @Override public boolean isHidden() { return true; }
    @Override public PermissionDefault getPermissionDefault() { return PermissionDefault.OP; }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int i, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl verwenden.");
            return;
        }

        GroupEditSession session = GroupEditSession.getSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("Du bearbeitest aktuell keine Gruppe.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Bitte gib ein Item an, das du entfernen möchtest.", NamedTextColor.RED));
            return;
        }

        String itemName = args[0].toUpperCase(Locale.ROOT);
        if (!session.hasItem(itemName)) {
            player.sendMessage(Component.text("Das Item ist nicht in der Liste: " + itemName, NamedTextColor.RED));
            return;
        }

        ConfirmationPrompt.send(player,
                "Möchtest du das Item '" + itemName + "' wirklich entfernen?",
                () -> {
                    session.removeItem(itemName);
                    player.sendMessage(Component.text("Item entfernt: " + itemName, NamedTextColor.YELLOW));
                    // sendEditorMessage wird in removeItem aufgerufen
                },
                () -> {
                    player.sendMessage(Component.text("Abgebrochen.", NamedTextColor.GRAY));
                    session.sendEditorMessage(); // Menü nochmal zeigen
                });
    }
}