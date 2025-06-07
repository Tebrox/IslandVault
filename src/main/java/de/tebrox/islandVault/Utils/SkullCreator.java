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
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkullCreator {

    private static final boolean USE_NEW_API = isNewApi();

    private static final Map<String, ItemStack> cache = new ConcurrentHashMap<>();

    /**
     * Erzeugt einen Skull aus einer Mojang-UUID.
     */
    public static ItemStack fromUuid(UUID uuid) {
        String key = "uuid:" + uuid;
        return cache.computeIfAbsent(key, k -> {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta == null) return skull;

            if (USE_NEW_API) {
                try {
                    Method createProfile = Bukkit.class.getMethod("createProfile", UUID.class);
                    Object profile = createProfile.invoke(null, uuid);
                    Method setProfile = meta.getClass().getMethod("setOwnerProfile", profile.getClass());
                    setProfile.invoke(meta, profile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                meta.setOwningPlayer(player);
            }

            skull.setItemMeta(meta);
            return skull;
        });
    }

    /**
     * Erzeugt einen Skull aus einer Skin-URL.
     */
    public static ItemStack fromUrl(String url) {
        return fromBase64(urlToBase64(url));
    }

    /**
     * Erzeugt einen Skull aus einem Base64-Texturwert.
     */
    public static ItemStack fromBase64(String base64) {
        String key = "base64:" + base64;
        return cache.computeIfAbsent(key, k -> {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta == null) return skull;

            try {
                // Paper API
                PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(base64.getBytes()));
                profile.setProperty(new ProfileProperty("textures", base64));
                meta.setOwnerProfile(profile);
            } catch (NoClassDefFoundError e) {
                // Kein Paper? Dann alter Weg (Reflection auf GameProfile)
                try {
                    UUID uuid = UUID.nameUUIDFromBytes(base64.getBytes());
                    GameProfile profile = new GameProfile(uuid, "Skull");
                    profile.getProperties().put("textures", new Property("textures", base64));

                    Field profileField = meta.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(meta, profile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            skull.setItemMeta(meta);
            return skull;
        });
    }

    /**
     * Konvertiert eine Skin-URL zu einem Base64-Wert.
     */
    public static String urlToBase64(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    private static boolean isNewApi() {
        try {
            SkullMeta meta = (SkullMeta) new ItemStack(Material.PLAYER_HEAD).getItemMeta();
            if (meta == null) return false;
            for (Method m : meta.getClass().getMethods()) {
                if (m.getName().equals("setOwnerProfile")) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}