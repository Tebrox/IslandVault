package de.tebrox.islandVault.Tasks;

import de.tebrox.islandVault.IslandVault;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Map;

public class RadiusTask extends ParticleTask {

    private final Location centerBlock;
    private final int radius;
    private final int minX, maxX, minZ, maxZ, minY, maxY;
    private final int totalTicks;
    private int ticksPassed = 0;

    public RadiusTask(Player player, Location centerBlock, int radius,
                      int minX, int maxX, int minZ, int maxZ, int minY, int maxY,
                      int seconds, Particle particle, @Nullable Color colorFrom, @Nullable Color colorTo) {
        super(player, particle, colorFrom, colorTo);
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

        for (int y = minY; y <= maxY; y++) {
            for (int x = x1; x <= x2; x++) {
                spawnParticle(new Location(centerBlock.getWorld(), x + 0.5, y + 0.1, z1 + 0.5));
                spawnParticle(new Location(centerBlock.getWorld(), x + 0.5, y + 0.1, z2 + 0.5));
            }
            for (int z = z1 + 1; z < z2; z++) {
                spawnParticle(new Location(centerBlock.getWorld(), x1 + 0.5, y + 0.1, z + 0.5));
                spawnParticle(new Location(centerBlock.getWorld(), x2 + 0.5, y + 0.1, z + 0.5));
            }
        }

        if (ticksPassed % 20 == 0) {
            int secondsLeft = (totalTicks - ticksPassed) / 20;
            String msg = IslandVault.getLanguageManager().translate(player, "radiusActionbarTimer", Map.of("secondsLeft", String.valueOf(secondsLeft)), true);
            player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
        }

        ticksPassed += 2;
    }
}