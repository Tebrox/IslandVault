package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.Manager.PendingActionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ConfirmationPrompt {

    public static void send(Player player, String message, Runnable onConfirm, Runnable onCancel) {
        UUID uuid = player.getUniqueId();

        PendingActionManager.registerPendingAction(uuid, onConfirm);
        PendingActionManager.registerCancelAction(uuid, onCancel);

        Component confirm = Component.text("[✔ Ja]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/vaultadmin confirm"))
                .hoverEvent(HoverEvent.showText(Component.text("Bestätigen")));

        Component cancel = Component.text("[✖ Nein]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/vaultadmin cancel"))
                .hoverEvent(HoverEvent.showText(Component.text("Abbrechen")));

        player.sendMessage(Component.text(message).color(NamedTextColor.RED));
        player.sendMessage(Component.empty().append(confirm).append(Component.text("    ")).append(cancel));
    }
}

