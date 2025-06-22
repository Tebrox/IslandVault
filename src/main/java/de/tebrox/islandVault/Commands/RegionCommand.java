package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Listeners.RegionListener;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.IslandTracker;
import de.tebrox.islandVault.Manager.RegionManager;
import de.tebrox.islandVault.Region.Region;
import de.tebrox.islandVault.Region.RegionSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import static net.kyori.adventure.text.Component.text;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                System.out.println(Arrays.toString(args));
                if (firstArg.equals("delete") || firstArg.equals("edit")) {
                    return IslandVault.getRegionManager().getRegionNames(IslandTracker.getPlayerIsland(player.getUniqueId()).getUniqueId());
                }
            }
        }
        return List.of();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        for(String s : args) {
            System.out.println(s);
        }
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
                player.sendMessage("§7Wähle zwei Positionen aus und schreibe §e'fertig'§7 in den Chat.");
            }
            case "edit" -> {
                String regionName = args[1];

                if (IslandVault.getRegionManager().getRegion(islandUUID, regionName) == null) {
                    player.sendMessage("§cDiese Region existiert nicht.");
                    return;
                }

                RegionSession session = new RegionSession(regionName, RegionSession.ActionType.EDIT);
                IslandVault.getRegionManager().startSession(player.getUniqueId(), session);
                player.sendMessage("§7Wähle neue Positionen und schreibe §e'fertig'§7 in den Chat.");
            }
            case "delete" -> {
                String regionName = args[1];
                if (!IslandVault.getRegionManager().regionExists(islandUUID, regionName)) {
                    player.sendMessage("§cDiese Region existiert nicht.");
                    return;
                }
                IslandVault.getRegionManager().removeRegion(islandUUID, regionName);
                player.sendMessage("§aRegion \"" + regionName + "\" gelöscht.");
            }
            case "list" -> {
                List<Region> regions = IslandVault.getRegionManager().getRegions(islandUUID);
                if (regions.isEmpty()) {
                    player.sendMessage("§7Deine Insel hat noch keine Regionen.");
                    return;
                }

                player.sendMessage("§aRegionen deiner Insel:");

                for (Region region : regions) {
                    String regionName = region.getName();
                    Location pos1 = region.getPos1();
                    Location pos2 = region.getPos2();
                    String world = pos1.getWorld().getName();

                    Component regionLine = text("- ")
                            .color(NamedTextColor.GRAY)
                            .append(text(regionName).color(NamedTextColor.YELLOW))
                            .append(text(" [Anzeigen]").color(NamedTextColor.DARK_GRAY)
                                    .hoverEvent(HoverEvent.showText(text("§7Partikelvorschau anzeigen")))
                                    .clickEvent(ClickEvent.runCommand("/region show " + regionName)));

                    player.sendMessage(regionLine);
                }
            }
            case "show" -> {
                String regionName = args[1];
                Region region = IslandVault.getRegionManager().getRegion(islandUUID, regionName);
                if (region == null) {
                    player.sendMessage("§cRegion nicht gefunden.");
                    return;
                }

                player.sendMessage("§aPartikelvorschau für §e" + regionName + " §awird angezeigt...");

                new BukkitRunnable() {
                    int ticks = 60;
                    @Override
                    public void run() {
                        if (ticks-- <= 0 || !player.isOnline()) {
                            cancel();
                            return;
                        }
                        IslandVault.getParticleManager().showBox(player, "regionBox", region.getPos1(), region.getPos2(), Particle.DUST, Color.GREEN, null);
                    }
                }.runTaskTimer(IslandVault.getPlugin(), 0L, 10L);
            }
            default -> player.sendMessage("§cUnbekannter Befehl.");
        }
    }



    private String locString(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
