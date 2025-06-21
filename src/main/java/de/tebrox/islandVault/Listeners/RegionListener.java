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
import org.bukkit.util.Vector;

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
            e.setCancelled(true);
            session.setPos1(clicked);
            player.sendMessage("§aPosition 1 gesetzt: §e" + loc(clicked));
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            session.setPos2(clicked);
            player.sendMessage("§aPosition 2 gesetzt: §e" + loc(clicked));
        }

        showSelectionParticles(player.getUniqueId(), session);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        RegionSession session = IslandVault.getRegionManager().getSession(uuid);
        if (session == null) return;

        String msg = event.getMessage().trim().toLowerCase();
        switch (msg.toLowerCase()) {
            case "fertig":
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
                        IslandVault.getRegionManager().endSession(uuid);
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
                break;
            case "pos1":
                event.setCancelled(true);
                IslandVault.getRegionManager().getSession(player.getUniqueId()).setPos1(player.getLocation());
                break;
            case "pos2":
                event.setCancelled(true);
                IslandVault.getRegionManager().getSession(player.getUniqueId()).setPos2(player.getLocation());
                break;
            default:
        }
    }

    public static void showSelectionParticles(UUID playerId, RegionSession session) {
        Location pos1 = session.getPos1();
        Location pos2 = session.getPos2();
        Color color;

        if (pos1 == null && pos2 == null) {
            return; // Keine Position gesetzt, nichts anzeigen
        }

        if (pos1 != null && pos2 != null) {
            color = Color.GREEN;
        } else {
            color = Color.RED;
        }
        IslandVault.getParticleManager().showBox(Bukkit.getPlayer(playerId), "regionBox", pos1, pos2, Particle.DUST, color, null);
    }

    private String loc(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
