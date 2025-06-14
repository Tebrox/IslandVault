package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.Manager.ItemGroupManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class GroupEditSession {

    private static final Map<UUID, GroupEditSession> sessions = new HashMap<>();

    private final UUID playerUUID;
    private final String groupName;
    private final Set<String> items;
    private final Set<String> originalItems;

    public GroupEditSession(UUID playerUUID, String groupName, List<String> currentItems) {
        this.playerUUID = playerUUID;
        this.groupName = groupName;
        this.items = new LinkedHashSet<>(currentItems);
        this.originalItems = new LinkedHashSet<>(currentItems);
        sessions.put(playerUUID, this);
    }

    public static GroupEditSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public static void endSession(UUID uuid) {
        sessions.remove(uuid);
    }

    // Zeigt den Chat-Editor an
    public void sendEditorMessage() {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        player.sendMessage(Component.text("§7Bearbeite Gruppe §e" + groupName + "§7:"));

        for (String item : items) {
            Component line = Component.text("- " + item + " ")
                    .append(
                            Component.text("[✖]", NamedTextColor.RED)
                                    .hoverEvent(HoverEvent.showText(Component.text("Klicke zum Entfernen")))
                                    .clickEvent(ClickEvent.runCommand("/vaultadmin removeitemfromsession " + item))
                    );
            player.sendMessage(line);
        }

        player.sendMessage(Component.text("§eTippe ein neues Item (z.B. REDSTONE), oder"));
        player.sendMessage(Component.text("§a\"fertig\" §ezum Speichern, §c\"abbrechen\" §ezum Abbrechen."));
    }

    public void removeItem(String item) {
        items.remove(item);
        sendEditorMessage();
    }

    public void addItem(String item) {
        items.add(item);
        sendEditorMessage();
    }

    public boolean hasItem(String item) {
        return items.contains(item);
    }

    public boolean handleChatInput(String input) {
        String msg = input.trim();
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return true;

        if (msg.equalsIgnoreCase("fertig")) {
            ItemGroupManager.saveGroup(groupName, new ArrayList<>(items));
            player.sendMessage("§aGruppe §e" + groupName + " §awurde gespeichert.");
            endSession(playerUUID);
            return true;
        }

        if (msg.equalsIgnoreCase("abbrechen")) {
            player.sendMessage("§cBearbeitung abgebrochen.");
            endSession(playerUUID);
            return true;
        }

        try {
            Material.valueOf(msg.toUpperCase());
            addItem(msg.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cUngültiges Item: " + msg);
        }
        return false;
    }
}