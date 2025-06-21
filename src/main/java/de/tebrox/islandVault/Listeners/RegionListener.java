package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Region.Region;
import de.tebrox.islandVault.Region.RegionSession;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public class RegionListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player player = e.getPlayer();
        RegionSession session = IslandVault.getRegionManager().getSession(player.getUniqueId());
        if (session == null) return;

        Location clicked = e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : null;
        if (clicked == null) return;

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            session.setPos1(clicked);
            player.sendMessage("§aPosition 1 gesetzt: §e" + loc(clicked));
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            session.setPos2(clicked);
            player.sendMessage("§aPosition 2 gesetzt: §e" + loc(clicked));
        }

        showSelectionParticles(player, session);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        RegionSession session = IslandVault.getRegionManager().getSession(uuid);
        if (session == null) return;

        String msg = event.getMessage().trim().toLowerCase();
        if (!msg.equalsIgnoreCase("fertig")) return;

        event.setCancelled(true);

        if (session.getPos1() == null || session.getPos2() == null) {
            player.sendMessage("§cDu musst zuerst beide Positionen setzen.");
            return;
        }

        Bukkit.getScheduler().runTask(IslandVault.getPlugin(), () -> {
            String islandUUID = IslandVault.getRegionManager().getIslandUUID(player);
            String name = session.getName();

            if (session.getAction() == RegionSession.ActionType.CREATE) {
                if (IslandVault.getRegionManager().getRegion(islandUUID, name) != null) {
                    player.sendMessage("§cEs existiert bereits eine Region mit diesem Namen.");
                    IslandVault.getRegionManager().endSession(uuid);
                    return;
                }

                Region region = new Region(name, session.getPos1(), session.getPos2());
                IslandVault.getRegionManager().addRegion(islandUUID, region);
                player.sendMessage("§aRegion §e" + name + " §awurde erstellt.");
            } else if (session.getAction() == RegionSession.ActionType.EDIT) {
                Region region = IslandVault.getRegionManager().getRegion(islandUUID, name);
                if (region == null) {
                    player.sendMessage("§cRegion nicht gefunden.");
                    IslandVault.getRegionManager().endSession(uuid);
                    return;
                }

                region.setPos1(session.getPos1());
                region.setPos2(session.getPos2());
                player.sendMessage("§aRegion §e" + name + " §awurde bearbeitet.");
            }

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.2f);
            IslandVault.getRegionManager().endSession(uuid);
        });
    }

    public static void showSelectionParticles(Player player, RegionSession session) {
        Location pos1 = session.getPos1();
        Location pos2 = session.getPos2();

        if (pos1 != null && pos2 != null) {
            drawParticleOutline(player, pos1, pos2, Particle.HAPPY_VILLAGER);
        } else if (pos1 != null) {
            player.spawnParticle(Particle.DRIPPING_LAVA, pos1.add(0.5, 0.5, 0.5), 30,
                    0.3, 0.3, 0.3, new Particle.DustOptions(Color.RED, 1.0f));
        } else if (pos2 != null) {
            player.spawnParticle(Particle.DRIPPING_LAVA, pos2.add(0.5, 0.5, 0.5), 30,
                    0.3, 0.3, 0.3, new Particle.DustOptions(Color.RED, 1.0f));
        }
    }

    public static void drawParticleOutline(Player player, Location pos1, Location pos2, Particle particle) {
        World world = pos1.getWorld();
        if (!pos2.getWorld().equals(world)) return;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onEdge = (x == minX || x == maxX ||
                            y == minY || y == maxY ||
                            z == minZ || z == maxZ);

                    if (onEdge) {
                        Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                        player.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    private String loc(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
