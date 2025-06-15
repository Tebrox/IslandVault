package de.tebrox.islandVault.Utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkullCreator {

    private static final Map<String, ItemStack> cache = new ConcurrentHashMap<>();

    /**
     * Erzeugt einen Skull anhand eines Base64-Strings.
     */
    public static ItemStack fromBase64(String base64) {
        return cache.computeIfAbsent("base64:" + base64, key -> createSkull(base64));
    }

    /**
     * Erzeugt einen Skull anhand einer Skin-URL.
     */
    public static ItemStack fromURL(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes());
        return fromBase64(base64);
    }

    /**
     * Erzeugt einen Skull anhand eines Spielernamens.
     */
    public static ItemStack fromName(String name) {
        return cache.computeIfAbsent("name:" + name, key -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            return fromPlayer(player);
        });
    }

    /**
     * Erzeugt einen Skull anhand einer UUID.
     */
    public static ItemStack fromUUID(UUID uuid) {
        return cache.computeIfAbsent("uuid:" + uuid.toString(), key -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return fromPlayer(player);
        });
    }

    /**
     * Erstellt einen Skull mit Textur für einen Spieler.
     */
    public static ItemStack fromPlayer(OfflinePlayer player) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Erstellt einen Skull mit Base64-Textur ohne Spielerbindung.
     */
    private static ItemStack createSkull(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        try {
            if (isPaper()) {
                // Für Paper oder Forks mit PlayerProfile API
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                profile.getProperties().add(new ProfileProperty("textures", base64));
                meta.setPlayerProfile(profile);
            } else {
                // Für Spigot & andere: Reflektion
                GameProfile profile = new GameProfile(UUID.randomUUID(), null);
                profile.getProperties().put("textures", new Property("textures", base64));

                Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Leert den Cache manuell (optional).
     */
    public static void clearCache() {
        cache.clear();
    }

    private static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}