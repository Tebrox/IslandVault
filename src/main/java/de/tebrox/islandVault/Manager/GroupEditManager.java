package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.IslandVault;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class GroupEditManager {

    private static final Map<UUID, EditSession> editingPlayers = new HashMap<>();

    public static void startEditing(Player player, String groupName) {
        UUID uuid = player.getUniqueId();

        if (editingPlayers.containsKey(uuid)) {
            player.sendMessage("Du bearbeitest bereits eine Gruppe. Beende diese zuerst mit /vaultadmin cancel.");
            return;
        }

        List<String> currentItems = new ArrayList<>(ItemGroupManager.getPermissionGroups().getOrDefault(groupName, Collections.emptyList()));
        EditSession session = new EditSession(groupName, currentItems, player);
        editingPlayers.put(uuid, session);

        session.sendEditStatus();

        // Start Timer für Timeout (z.B. 2 Minuten)
        session.startTimeoutCountdown(() -> {
            player.sendMessage("Deine Bearbeitung der Gruppe '" + groupName + "' wurde wegen Inaktivität beendet.");
            editingPlayers.remove(uuid);
        });
    }

    public static boolean isEditing(Player player) {
        return editingPlayers.containsKey(player.getUniqueId());
    }

    public static EditSession getSession(Player player) {
        return editingPlayers.get(player.getUniqueId());
    }

    public static void stopEditing(Player player, boolean save) {
        UUID uuid = player.getUniqueId();
        EditSession session = editingPlayers.remove(uuid);
        if (session == null) return;

        if (save) {
            ItemGroupManager.saveGroup(session.groupName, session.items);
            player.sendMessage("Die Gruppe '" + session.groupName + "' wurde gespeichert.");
        } else {
            player.sendMessage("Die Bearbeitung wurde abgebrochen, keine Änderungen gespeichert.");
        }
    }

    public static class EditSession {

        private final String groupName;
        private final List<String> items;
        private final Player player;
        private BukkitTask timeoutTask;
        private int timeoutSeconds = 120; // 2 Minuten Timeout

        public EditSession(String groupName, List<String> currentItems, Player player) {
            this.groupName = groupName;
            this.items = new ArrayList<>(currentItems);
            this.player = player;
        }

        public void sendEditStatus() {
            player.sendMessage("Bearbeite Gruppe '" + groupName + "'. Aktuelle Items:");
            for (String item : items) {
                player.sendMessage(formatItemLine(item));
            }
            player.sendMessage("Schreibe Item-Namen in den Chat, um hinzuzufügen.");
            player.sendMessage("Zum Löschen eines Items schreibe den Item-Namen gefolgt von [x], z.B. 'DIAMOND[x]'.");
            player.sendMessage("Schreibe 'fertig' zum Speichern oder 'abbrechen' zum Verwerfen.");
        }

        private String formatItemLine(String itemName) {
            Component itemText = Component.text(itemName, NamedTextColor.YELLOW);
            Component deleteButton = Component.text("[x]", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/vaultadmin removeitemconfirm " + itemName))
                    .hoverEvent(HoverEvent.showText(Component.text("Klicke zum Entfernen")));

            String legacy = LegacyComponentSerializer.legacySection().serialize(
                    Component.text().append(itemText).append(Component.space()).append(deleteButton).build()
            );

            return legacy;
        }

        public void addItem(String item) {
            if (!items.contains(item)) {
                items.add(item);
                player.sendMessage("Item '" + item + "' hinzugefügt.");
                sendEditStatus();
                resetTimeout();
            } else {
                player.sendMessage("Item '" + item + "' ist bereits in der Gruppe.");
            }
        }

        public boolean hasItem(String item) {
            return items.contains(item);
        }

        public void removeItem(String item) {
            if (items.remove(item)) {
                player.sendMessage("Item '" + item + "' entfernt.");
                sendEditStatus();
                resetTimeout();
            } else {
                player.sendMessage("Item '" + item + "' war nicht in der Gruppe.");
            }
        }

        public void startTimeoutCountdown(Runnable onTimeout) {
            timeoutTask = Bukkit.getScheduler().runTaskTimer(IslandVault.getPlugin(), () -> {
                if (timeoutSeconds <= 0) {
                    timeoutTask.cancel();
                    onTimeout.run();
                } else {
                    timeoutSeconds--;
                    if (timeoutSeconds <= 10) {
                        player.sendActionBar(Component.text("Bearbeitung endet in " + timeoutSeconds + " Sekunden...", NamedTextColor.RED));
                    }
                }
            }, 20L, 20L);
        }

        public void resetTimeout() {
            timeoutSeconds = 120;
        }

        public void cancelTimeout() {
            if (timeoutTask != null) {
                timeoutTask.cancel();
            }
        }
    }
}