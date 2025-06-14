package de.tebrox.islandVault.Commands.AdminCommand.ItemGroups;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.GroupEditManager;
import de.tebrox.islandVault.Manager.ItemGroupManager;
import de.tebrox.islandVault.Manager.ItemManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class EditGroupChatCommand implements SubCommand {

    @Override
    public String getName() {
        return "editgroup";
    }

    @Override
    public String getDescription() {
        return "Bearbeitet eine Item-Gruppe im Chat.";
    }

    @Override
    public String getSyntax() {
        return "/vaultadmin editgroup <Gruppenname>";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.OP;
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        if (index == 0) {
            // Gruppen-Namen vorschlagen
            return new ArrayList<>(ItemGroupManager.getPermissionGroups().keySet());
        }
        if (index > 0) {
            // Vorschlag für Materialien, wenn Item(s) hinzugefügt werden
            String lastArg = args[args.length - 1].toUpperCase();
            return IslandVault.getItemManager().getMaterialList().stream()
                    .map(Material::name)
                    .filter(name -> name.startsWith(lastArg))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur ingame ausgeführt werden.");
            return;
        }
        if (args.length < 1) {
            player.sendMessage("Bitte gib eine Gruppenname an. Syntax: " + getSyntax());
            return;
        }

        String groupName = args[0].toLowerCase();
        if (!ItemGroupManager.getPermissionGroups().containsKey(groupName)) {
            player.sendMessage("Die Gruppe '" + groupName + "' existiert nicht.");
            return;
        }

        GroupEditManager.startEditing(player, groupName);
    }
}