package de.tebrox.islandVault.Tasks;

import de.tebrox.islandVault.IslandVault;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Map;

public class RadiusTask extends ParticleTask {

    private final Location centerBlock;
    private final int radius;
    private final int minX, maxX, minZ, maxZ, minY, maxY;
    private final int totalTicks;
    private int ticksPassed = 0;
    private float size = 0.8f;

    public RadiusTask(Player player, Location centerBlock, int radius,
                      int minX, int maxX, int minZ, int maxZ, int minY, int maxY,
                      int seconds, Particle particle, @Nullable Color colorFrom, @Nullable Color colorTo) {
        super(player, player.getWorld(), particle, colorFrom, colorTo);
        this.centerBlock = centerBlock;
        this.radius = radius;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.minY = minY;
        this.maxY = maxY;
        this.totalTicks = seconds * 20;
    }

    @Override
    public void run() {
        if (ticksPassed >= totalTicks) {
            String message = IslandVault.getLanguageManager().translate(player, "radiusActionbarTimer", Map.of("secondsLeft", "0"), false);
            player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            this.cancel();
            return;
        }

        int cx = centerBlock.getBlockX();
        int cz = centerBlock.getBlockZ();

        int x1 = Math.max(cx - radius, minX);
        int x2 = Math.min(cx + radius, maxX);
        int z1 = Math.max(cz - radius, minZ);
        int z2 = Math.min(cz + radius, maxZ);

        showBlockMarkerEdges(world, x1, x2, z1, z2, minY, maxY, player);

        if (ticksPassed % 20 == 0) {
            int secondsLeft = (totalTicks - ticksPassed) / 20;
            String msg = IslandVault.getLanguageManager().translate(player, "radiusActionbarTimer", Map.of("secondsLeft", String.valueOf(secondsLeft)), true);
            player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
        }

        ticksPassed += 2;
    }

    public void showBlockMarkerEdges(World world, int x1, int x2, int z1, int z2, int minY, int maxY, Player player) {
        BlockData markerBlock = Material.GLASS_PANE.createBlockData();  // Beispielblock für Marker

        for (int y = minY; y <= maxY; y++) {
            // Vorderkante (Z = z1), von x1 bis x2
            for (int x = x1; x <= x2; x++) {
                world.spawnParticle(Particle.BLOCK_MARKER, x + 0.5, y + 0.5, z1 + 0.5,
                        1, 0, 0, 0, 0, markerBlock, true);
            }

            // Rückkante (Z = z2), von x1 bis x2
            for (int x = x1; x <= x2; x++) {
                world.spawnParticle(Particle.BLOCK_MARKER, x + 0.5, y + 0.5, z2 + 0.5,
                        1, 0, 0, 0, 0, markerBlock, true);
            }

            // Linke Kante (X = x1), von z1 bis z2
            for (int z = z1; z <= z2; z++) {
                world.spawnParticle(Particle.BLOCK_MARKER, x1 + 0.5, y + 0.5, z + 0.5,
                        1, 0, 0, 0, 0, markerBlock, true);
            }

            // Rechte Kante (X = x2), von z1 bis z2
            for (int z = z1; z <= z2; z++) {
                world.spawnParticle(Particle.BLOCK_MARKER, x2 + 0.5, y + 0.5, z + 0.5,
                        1, 0, 0, 0, 0, markerBlock, true);
            }
        }
    }
}