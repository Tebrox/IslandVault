package de.tebrox.islandVault.Tasks;

import de.tebrox.islandVault.IslandVault;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Map;

public class BoxTask extends ParticleTask {

    private final Location pos1, pos2;
    private final int totalTicks;
    private int ticksPassed = 0;
    private final boolean show;
    private final float size;

    public BoxTask(Player player, Location pos1, Location pos2, Particle particle, @Nullable Color colorFrom, @Nullable Color colorTo, boolean show) {
        super(player, (pos1 != null ? pos1.getWorld() : pos2.getWorld()), particle, colorFrom, colorTo);
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.show = show;
        this.totalTicks = 10 * 20;
        this.size = 0.4f;
    }

    @Override
    public void run() {
        if (pos1 != null && pos2 != null) {
            if(!show) {
                drawBox(pos1, pos2);
            }else{
                if (ticksPassed >= totalTicks) {
                    String message = IslandVault.getLanguageManager().translate(player, "radiusActionbarTimer", Map.of("secondsLeft", "0"), false);
                    player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                    this.cancel();
                    return;
                }

                drawBox(pos1, pos2);

                if (ticksPassed % 20 == 0) {
                    int secondsLeft = (totalTicks - ticksPassed) / 20;
                    String msg = IslandVault.getLanguageManager().translate(player, "radiusActionbarTimer", Map.of("secondsLeft", String.valueOf(secondsLeft)), true);
                    player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                }

                ticksPassed += 2;
            }
        } else if (pos1 != null) {
            drawBox(pos1, pos1);
        } else if (pos2 != null) {
            drawBox(pos2, pos2);
        }
    }

    private void drawBox(Location a, Location b) {
        double step = 0.3;

        double minX = Math.min(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double minZ = Math.min(a.getZ(), b.getZ());
        double maxX = Math.max(a.getX(), b.getX()) + 1.0; // +1 für exakte Blockbegrenzung
        double maxY = Math.max(a.getY(), b.getY()) + 1.0;
        double maxZ = Math.max(a.getZ(), b.getZ()) + 1.0;

        boolean singleBlock = minX == maxX - 1.0 && minY == maxY - 1.0 && minZ == maxZ - 1.0;

        if (singleBlock) {
            drawCubeEdges(new Location(world, minX, minY, minZ), step);
            return;
        }

        // 8 Eckpunkte
        double x1 = minX, x2 = maxX;
        double y1 = minY, y2 = maxY;
        double z1 = minZ, z2 = maxZ;

        // 12 Kanten des Quaders
        drawLine(world, x1, y1, z1, x2, y1, z1, step, size);
        drawLine(world, x1, y1, z1, x1, y2, z1, step, size);
        drawLine(world, x1, y1, z1, x1, y1, z2, step, size);

        drawLine(world, x2, y2, z2, x1, y2, z2, step, size);
        drawLine(world, x2, y2, z2, x2, y1, z2, step, size);
        drawLine(world, x2, y2, z2, x2, y2, z1, step, size);

        drawLine(world, x2, y1, z1, x2, y2, z1, step, size);
        drawLine(world, x2, y1, z1, x2, y1, z2, step, size);
        drawLine(world, x1, y2, z1, x2, y2, z1, step, size);
        drawLine(world, x1, y2, z1, x1, y2, z2, step, size);
        drawLine(world, x1, y1, z2, x1, y2, z2, step, size);
        drawLine(world, x1, y1, z2, x2, y1, z2, step, size);
    }

    private void drawCubeEdges(Location origin, double step) {
        double x = origin.getX();
        double y = origin.getY();
        double z = origin.getZ();

        double x1 = x;
        double x2 = x + 1;
        double y1 = y;
        double y2 = y + 1;
        double z1 = z;
        double z2 = z + 1;

        // 12 Kanten des Würfels
        drawLine(world, x1, y1, z1, x2, y1, z1, step, size);
        drawLine(world, x1, y1, z1, x1, y2, z1, step, size);
        drawLine(world, x1, y1, z1, x1, y1, z2, step, size);

        drawLine(world, x2, y2, z2, x1, y2, z2, step, size);
        drawLine(world, x2, y2, z2, x2, y1, z2, step, size);
        drawLine(world, x2, y2, z2, x2, y2, z1, step, size);

        drawLine(world, x2, y1, z1, x2, y2, z1, step, size);
        drawLine(world, x2, y1, z1, x2, y1, z2, step, size);
        drawLine(world, x1, y2, z1, x2, y2, z1, step, size);
        drawLine(world, x1, y2, z1, x1, y2, z2, step, size);
        drawLine(world, x1, y1, z2, x1, y2, z2, step, size);
        drawLine(world, x1, y1, z2, x2, y1, z2, step, size);
    }
}