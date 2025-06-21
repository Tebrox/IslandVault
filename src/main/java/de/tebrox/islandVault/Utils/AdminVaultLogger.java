package de.tebrox.islandVault.Utils;

import de.tebrox.islandVault.Enums.LoggerAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class AdminVaultLogger {
    private final File logFile;

    public AdminVaultLogger(JavaPlugin plugin) {
        File folder = new File(plugin.getDataFolder(), "logs");
        if (!folder.exists()) folder.mkdirs();
        logFile = new File(folder, "vault-actions.log");
    }

    public void logAction(LoggerAction action, Player actor, UUID vaultOwner, ItemStackKey itemStackKey, int amount) {
        String ownerName = Bukkit.getOfflinePlayer(vaultOwner).getName();

        String a = "";
        switch (action) {
            case LoggerAction.ADD_ITEM:
                a = "hinzugefügt";
                break;
            case LoggerAction.REMOVE_ITEM:
                a = "entnommen";
                break;
        }


        String log = String.format("[%s] Insellager von: %s. %s hat %dx %s %s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
            ownerName != null ? ownerName : vaultOwner.toString(),
            actor.getName(),
            amount,
            itemStackKey.toItemStack().getType(),
            a
        );

        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(log);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
