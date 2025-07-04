package de.tebrox.islandVault.Tasks;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;

public abstract class ParticleTask extends BukkitRunnable {
    protected final Player player;
    protected final World world;
    protected final Particle particle;
    protected final Color colorFrom;
    protected final Color colorTo;

    protected ParticleTask(Player player, World world, Particle particle, @Nullable Color colorFrom, @Nullable Color colorTo) {
        this.player = player;
        this.world = world;
        this.particle = particle;
        this.colorFrom = colorFrom != null ? colorFrom : Color.RED;
        this.colorTo = colorTo != null ? colorTo : Color.RED;
    }

    protected void spawnParticle(Location location) {
        spawnParticle(location, 1f);
    }

    protected void spawnParticle(Location location, float size) {
        int count = 5;
        double offset = 0.01;

        switch (particle) {
            case BLOCK_MARKER:
                BlockData data = Material.REDSTONE_BLOCK.createBlockData(); // oder ein anderer Block
                player.spawnParticle(Particle.BLOCK_MARKER, location, 1, 0, 0, 0, 0, data);
                break;
            case DUST:
                Particle.DustOptions dust = new Particle.DustOptions(colorFrom, size);
                player.spawnParticle(Particle.DUST, location, count, offset, offset, offset, 0, dust);
                break;

            case DUST_COLOR_TRANSITION:
                if (colorTo == null) return;
                Particle.DustTransition transition = new Particle.DustTransition(colorFrom, colorTo, size);
                player.spawnParticle(Particle.DUST_COLOR_TRANSITION, location, count, offset, offset, offset, 0, transition);
                break;

            default:
                player.spawnParticle(particle, location, count, offset, offset, offset, 0);
                break;
        }
    }

    public void drawLine(World world, double x1, double y1, double z1, double x2, double y2, double z2, double step, float size) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int points = (int) (distance / step);

        // Offset, um Blocküberlagerung zu vermeiden
        double offset = 0.02;

        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;

            // Punkt auf der Linie mit kleinem Offset (in Richtung des Vektors normiert)
            double x = x1 + dx * t;
            double y = y1 + dy * t;
            double z = z1 + dz * t;

            // Normalisierte Richtung für Offset (wenn distance=0 dann kein Offset)
            if (distance > 0) {
                double nx = dx / distance;
                double ny = dy / distance;
                double nz = dz / distance;

                x += nx * offset;
                y += ny * offset;
                z += nz * offset;
            } else {
                // Wenn Start- und Endpunkt gleich: versetze minimal nach oben
                y += offset;
            }

            spawnParticle(new Location(world, x, y, z), size);
        }
    }
}