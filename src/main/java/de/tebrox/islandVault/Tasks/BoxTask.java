package de.tebrox.islandVault.Tasks;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class BoxTask extends ParticleTask {

    private final Location pos1, pos2;
    private final World world;

    public BoxTask(Player player, Location pos1, Location pos2, Particle particle, @Nullable Color colorFrom, @Nullable Color colorTo) {
        super(player, particle, colorFrom, colorTo);
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.world = (pos1 != null) ? pos1.getWorld() : pos2.getWorld();
    }

    @Override
    public void run() {
        if (pos1 != null && pos2 != null) {
            drawBox(pos1, pos2);
        } else if (pos1 != null) {
            drawBox(pos1, pos1);
        } else if (pos2 != null) {
            drawBox(pos2, pos2);
        }
    }

    private void drawBox(Location a, Location b) {
        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxX = Math.max(a.getBlockX(), b.getBlockX());
        int maxY = Math.max(a.getBlockY(), b.getBlockY());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            spawnParticle(new Location(world, x, minY, minZ), 0.2f);
            spawnParticle(new Location(world, x, minY, maxZ), 0.2f);
            spawnParticle(new Location(world, x, maxY, minZ), 0.2f);
            spawnParticle(new Location(world, x, maxY, maxZ), 0.2f);
        }

        // Y-Kanten
        for (int y = minY; y <= maxY; y++) {
            spawnParticle(new Location(world, minX, y, minZ), 0.2f);
            spawnParticle(new Location(world, minX, y, maxZ), 0.2f);
            spawnParticle(new Location(world, maxX, y, minZ), 0.2f);
            spawnParticle(new Location(world, maxX, y, maxZ), 0.2f);
        }

        // Z-Kanten
        for (int z = minZ; z <= maxZ; z++) {
            spawnParticle(new Location(world, minX, minY, z), 0.2f);
            spawnParticle(new Location(world, minX, maxY, z), 0.2f);
            spawnParticle(new Location(world, maxX, minY, z), 0.2f);
            spawnParticle(new Location(world, maxX, maxY, z), 0.2f);
        }
    }


    private void spawn(Location loc) {
        spawnParticle(loc);
    }
}