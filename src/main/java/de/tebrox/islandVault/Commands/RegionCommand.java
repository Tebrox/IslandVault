package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.IslandTracker;
import de.tebrox.islandVault.Region.Region;
import de.tebrox.islandVault.Region.RegionSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import static net.kyori.adventure.text.Component.text;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RegionCommand implements SubCommand {
    @Override
    public String getName() {
        return "region";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getSyntax() {
        return "/insellager region <create|edit|delete> <name> ";
    }

    @Override
    public String getPermission() {
        return "";
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return null;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        if (index == 0) {
            return List.of("create", "edit", "delete", "list");
        } else if (index == 1) {
            if (args.length > 0 && sender instanceof Player player) {
                String firstArg = args[1].toLowerCase();
                if (firstArg.equals("delete") || firstArg.equals("edit")) {
                    return IslandVault.getRegionManager().getRegionNames(IslandTracker.getPlayerIsland(player.getUniqueId()).getUniqueId());
                }
            }
        }
        return List.of();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;

        String islandUUID = IslandVault.getRegionManager().getIslandUUID(player);

        if (args.length < 1 || (args.length < 2 && !args[0].equalsIgnoreCase("list"))) {
            player.sendMessage("§cVerwendung: /region <create|edit|delete|list> <name>");
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "create" -> {
                String regionName = args[1];

                if (IslandVault.getRegionManager().getRegion(islandUUID, regionName) != null) {
                    player.sendMessage("§cEs existiert bereits eine Region mit diesem Namen.");
                    return;
                }

                RegionSession session = new RegionSession(regionName, RegionSession.ActionType.CREATE);
                IslandVault.getRegionManager().startSession(player.getUniqueId(), session);
                player.sendMessage("§aWähle Positionen mit Links- und Rechtsklick.\n" +
                        "§aAlternativ gib §6'pos1'§a oder §6'pos2'§a im Chat ein.\n" +
                        "§aSchreibe §6'fertig'§a, um die Auswahl abzuschließen, oder §c'abbrechen'§a, um abzubrechen.");


            }
            case "edit" -> {
                String regionName = args[1];

                if (IslandVault.getRegionManager().getRegion(islandUUID, regionName) == null) {
                    player.sendMessage("§cDiese Region existiert nicht.");
                    return;
                }

                RegionSession session = new RegionSession(regionName, RegionSession.ActionType.EDIT);
                IslandVault.getRegionManager().startSession(player.getUniqueId(), session);
                player.sendMessage("§aWähle neue Positionen mit Links- und Rechtsklick oder gib §6'pos1'§a bzw. §6'pos2'§a im Chat ein.\n" +
                        "§aSchreibe §6'fertig'§a, sobald du die Auswahl abgeschlossen hast.");



            }
            case "delete" -> {
                String regionName = args[1];
                if (!IslandVault.getRegionManager().regionExists(islandUUID, regionName)) {
                    player.sendMessage("§cDie Region §6" + regionName + " §cexistiert nicht.");
                    return;
                }
                IslandVault.getRegionManager().removeRegion(islandUUID, regionName);
                player.sendMessage("§aRegion §6" + regionName + "§a gelöscht.");
            }
            case "list" -> {
                List<Region> regions = IslandVault.getRegionManager().getRegions(islandUUID);
                if (regions.isEmpty()) {
                    player.sendMessage("§cDeine Insel hat noch keine Regionen.");
                    return;
                }

                player.sendMessage("§aRegionen deiner Insel:");

                for (Region region : regions) {
                    String regionName = region.getName();

                    Component regionLine = text("- ")
                            .color(NamedTextColor.GRAY)
                            .append(text(regionName).color(NamedTextColor.YELLOW))
                            .append(text(" [Anzeigen]").color(NamedTextColor.DARK_GRAY)
                                    .hoverEvent(HoverEvent.showText(text("§7Partikelvorschau anzeigen")))
                                    .clickEvent(ClickEvent.runCommand("/insellager region show " + regionName)));

                    player.sendMessage(regionLine);
                }
            }
            case "show" -> {
                String regionName = args[1];
                Region region = IslandVault.getRegionManager().getRegion(islandUUID, regionName);
                if (region == null) {
                    player.sendMessage("§cRegion §6" + regionName + "§c nicht gefunden.");
                    return;
                }

                player.sendMessage("§aPartikelvorschau für §e" + regionName + " §awird angezeigt...");

                IslandVault.getParticleManager().showBox(player, UUID.randomUUID().toString(), region.getPos1(), region.getPos2(), Particle.DUST, Color.GREEN, null, true);
            }
            default -> player.sendMessage("§cUnbekannter Befehl.");
        }
    }



    private String locString(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
