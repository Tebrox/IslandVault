package de.tebrox.islandVault.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ItemNameTranslator {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    // Optionaler Cache (nach Sprache und Material)
    private static final Map<String, Map<Material, String>> CACHE = new HashMap<>();

    /**
     * Gibt den lokalisierten Namen eines Items für einen Spieler zurück.
     * Nutzt Fallback, wenn keine Übersetzung verfügbar ist.
     */
    public static String getLocalizedName(ItemStack item, Player player) {
        if (item == null || item.getType() == Material.AIR) return "Unbekannt";

        Material material = item.getType();
        //String lang = player.locale().toLowerCase(Locale.ROOT); // z. B. de_de, en_us
        String lang = player.locale().getLanguage().toLowerCase(Locale.ROOT);

        // Cache verwenden
        if (CACHE.containsKey(lang) && CACHE.get(lang).containsKey(material)) {
            return CACHE.get(lang).get(material);
        }

        String result;
        try {
            String key = item.translationKey(); // z. B. item.minecraft.nether_star
            if (key == null) throw new NullPointerException();

            TranslatableComponent tc = Component.translatable(key);
            result = PLAIN.serialize(tc);

            // Falls das Ergebnis leer ist oder gleich dem Key → Fallback
            if (result == null || result.trim().isEmpty() || result.equalsIgnoreCase(key)) {
                result = fallbackName(material);
            }

        } catch (Exception e) {
            result = fallbackName(material);
        }

        // In Cache speichern
        CACHE.computeIfAbsent(lang, l -> new HashMap<>()).put(material, result);
        return result;
    }

    /**
     * Fallback: Schöner lesbarer Name aus Material
     */
    private static String fallbackName(Material mat) {
        return mat.name()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' '); // z. B. "nether star"
    }

}