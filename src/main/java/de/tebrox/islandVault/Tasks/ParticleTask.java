package de.tebrox.islandVault.Tasks;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;

public abstract class ParticleTask extends BukkitRunnable {
    protected final Player player;
    protected final Particle particle;
    protected final Color colorFrom;
    protected final Color colorTo;

    protected ParticleTask(Player player, Particle particle, @Nullable Color colorFrom, @Nullable Color colorTo) {
        this.player = player;
        this.particle = particle;
        this.colorFrom = colorFrom != null ? colorFrom : Color.RED;
        this.colorTo = colorTo != null ? colorTo : Color.RED;
    }

    protected void spawnParticle(Location location) {
        spawnParticle(location, 1f);
    }

    protected void spawnParticle(Location location, float size) {
        if (particle == Particle.DUST) {
            Particle.DustOptions dust = new Particle.DustOptions(colorFrom, size);
            player.spawnParticle(particle, location, 1, 0, 0, 0, 0, dust);
        } else if(particle == Particle.DUST_COLOR_TRANSITION) {
            Particle.DustTransition transition = new Particle.DustTransition(colorFrom, colorTo, size);
            player.spawnParticle(particle, location, 0, 0, 0, 0, 0, transition);
        }else {
            player.spawnParticle(particle, location, 1, 0, 0, 0, 0);
        }
    }
}