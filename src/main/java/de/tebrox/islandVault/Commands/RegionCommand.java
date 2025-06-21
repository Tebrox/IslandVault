package de.tebrox.islandVault.Commands;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Listeners.RegionListener;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Region.Region;
import de.tebrox.islandVault.Region.RegionSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import static net.kyori.adventure.text.Component.text;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;

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
    public List<String> getTabCompletion(int index, String[] args) {
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
        String name = args[1];

        switch (action) {
            case "create" -> {
                String regionName = args[1];

                if (IslandVault.getRegionManager().getRegion(islandUUID, regionName) != null) {
                    player.sendMessage("§cEs existiert bereits eine Region mit diesem Namen.");
                    return;
                }

                RegionSession session = new RegionSession(name, RegionSession.ActionType.CREATE);
                IslandVault.getRegionManager().startSession(player.getUniqueId(), session);
                player.sendMessage("§7Wähle zwei Positionen aus und schreibe §e'fertig'§7 in den Chat.");
            }
            case "edit" -> {
                String regionName = args[1];

                if (IslandVault.getRegionManager().getRegion(islandUUID, regionName) == null) {
                    player.sendMessage("§cDiese Region existiert nicht.");
                    return;
                }

                RegionSession session = new RegionSession(name, RegionSession.ActionType.EDIT);
                IslandVault.getRegionManager().startSession(player.getUniqueId(), session);
                player.sendMessage("§7Wähle neue Positionen und schreibe §e'fertig'§7 in den Chat.");
            }
            case "delete" -> {
                if (!IslandVault.getRegionManager().regionExists(islandUUID, name)) {
                    player.sendMessage("§cDiese Region existiert nicht.");
                    return;
                }
                IslandVault.getRegionManager().removeRegion(islandUUID, name);
                player.sendMessage("§aRegion \"" + name + "\" gelöscht.");
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
                            .append(text(name).color(NamedTextColor.YELLOW))
                            .append(text(" [Anzeigen]").color(NamedTextColor.DARK_GRAY)
                                    .hoverEvent(HoverEvent.showText(text("§7Partikelvorschau anzeigen")))
                                    .clickEvent(ClickEvent.runCommand("/region show " + regionName)));

                    player.sendMessage(regionLine);
                }
            }
            case "show" -> {

                Region region = IslandVault.getRegionManager().getRegion(islandUUID, name);
                if (region == null) {
                    player.sendMessage("§cRegion nicht gefunden.");
                    return;
                }

                player.sendMessage("§aPartikelvorschau für §e" + name + " §awird angezeigt...");

                new BukkitRunnable() {
                    int ticks = 60;
                    @Override
                    public void run() {
                        if (ticks-- <= 0 || !player.isOnline()) {
                            cancel();
                            return;
                        }
                        RegionListener.drawParticleOutline(player, region.getPos1(), region.getPos2(), Particle.HAPPY_VILLAGER);
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
