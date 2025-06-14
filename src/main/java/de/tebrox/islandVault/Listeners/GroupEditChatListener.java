package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.Manager.GroupEditManager;
import de.tebrox.islandVault.Utils.ConfirmationPrompt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class GroupEditChatListener implements Listener {
    private Plugin plugin;


    public GroupEditChatListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!GroupEditManager.isEditing(player)) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        GroupEditManager.EditSession session = GroupEditManager.getSession(player);

        // Synchronize with main thread for Bukkit API
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("fertig")) {
                session.cancelTimeout();
                GroupEditManager.stopEditing(player, true);
            } else if (msg.equalsIgnoreCase("abbrechen")) {
                session.cancelTimeout();
                GroupEditManager.stopEditing(player, false);
            } else if (msg.endsWith("[x]")) {
                String itemToRemove = msg.substring(0, msg.length() - 3).toUpperCase();

                if (session.hasItem(itemToRemove)) {
                    // Bestätigung über ConfirmationPrompt
                    ConfirmationPrompt.send(player, "Willst du '" + itemToRemove + "' wirklich entfernen?",
                            () -> {
                                session.removeItem(itemToRemove);
                            },
                            () -> {
                                player.sendMessage("Entfernen abgebrochen.");
                                session.sendEditStatus();
                            });
                } else {
                    player.sendMessage("Das Item '" + itemToRemove + "' ist nicht in der Gruppe.");
                }
            } else {
                String itemToAdd = msg.toUpperCase();
                try {
                    Material mat = Material.valueOf(itemToAdd);
                    session.addItem(itemToAdd);
                } catch (IllegalArgumentException ex) {
                    player.sendMessage("'" + msg + "' ist kein gültiges Item.");
                }
            }
        });
    }
}