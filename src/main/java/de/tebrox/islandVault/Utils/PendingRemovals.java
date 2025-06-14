package de.tebrox.islandVault.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PendingRemovals {
    private static final Map<UUID, String> map = new HashMap<>();

    public static void put(UUID uuid, String item) {
        map.put(uuid, item);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.sendMessage(Component.text("Möchtest du " + item + " entfernen?"));
            p.sendMessage(
                    Component.text("[✔ Ja]", NamedTextColor.GREEN)
                            .hoverEvent(HoverEvent.showText(Component.text("Entfernen")))
                            .clickEvent(ClickEvent.runCommand("/vaultadmin confirmremove"))
                            .append(Component.space())
                            .append(Component.text("[✘ Nein]", NamedTextColor.RED)
                                    .hoverEvent(HoverEvent.showText(Component.text("Abbrechen")))
                                    .clickEvent(ClickEvent.runCommand("/vaultadmin cancelremove")))
            );
        }
    }

    public static String get(UUID uuid) {
        return map.get(uuid);
    }

    public static void clear(UUID uuid) {
        map.remove(uuid);
    }

    public static void clearAll() {
        map.clear();
    }
}