package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.Enums.Commands;
import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.VaultManager;
import de.tebrox.islandVault.Utils.IslandUtils;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

import java.util.List;
import java.util.Optional;

public class BackpackCommand implements SubCommand {

    @Override
    public String getName() {
        return "backpack";
    }

    @Override
    public String getDescription() {
        return "Öffnet den gemeinsamen Insel-Rucksack.";
    }

    @Override
    public String getSyntax() {
        return Commands.MAIN_COMMAND.getLabel() + " backpack";
    }

    @Override
    public String getPermission() {
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
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden!");
            return;
        }

        if (!player.hasPermission(getPermission())) {
            player.sendMessage(IslandVault.getLanguageManager().translate(player, "noPermission"));
            return;
        }

        IslandsManager islandsManager = IslandUtils.getIslandManager();
        if (islandsManager == null) {
            player.sendMessage("§cFehler: Insel-System (BentoBox) nicht verfügbar!");
            return;
        }

        // Insel anhand der aktuellen Position ermitteln
        Optional<Island> optIsland = IslandUtils.getIslandManager().getIslandAt(player.getLocation());
        if (optIsland.isEmpty()) {
            player.sendMessage(IslandVault.getLanguageManager().translate(player, "notOnIsland"));
            return;
        }

        Island island = optIsland.get();

        // Prüfen, ob der Spieler Zugriff auf diese Insel hat
        if (!IslandUtils.hasAccessToCurrentIsland(player)) {
            player.sendMessage(IslandVault.getLanguageManager().translate(player, "notTeamMember"));
            return;
        }

        VaultManager vaultManager = IslandVault.getVaultManager();

        // Insel-Vault laden, falls nicht vorhanden
        if (!vaultManager.getIslandVaults().containsKey(island.getUniqueId())) {
            vaultManager.loadIslandVault(island.getUniqueId());
            player.sendMessage("§cDas Insel-Lager wird derzeit geladen, bitte versuche es gleich erneut!");
            return;
        }

        // Backpack öffnen
        player.openInventory(vaultManager.getIslandVaults().get(island.getUniqueId()).getBackpack());
        player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }
}
