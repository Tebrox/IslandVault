package de.tebrox.islandVault.Utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtils {
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        if(PlayerDataUtils.loadSoundEnabled(player)) {
            player.playSound(player, sound, volume, pitch);
        }
    }
}
