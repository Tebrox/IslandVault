package de.tebrox.islandVault.Commands.AdminCommand;

import de.tebrox.islandVault.Enums.Permissions;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.CommandManager.SubCommand;
import de.tebrox.islandVault.Manager.ConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import world.bentobox.bentobox.database.objects.Island;

import java.util.List;
import java.util.Map;

public class DebugCommand implements SubCommand {
    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public String getDescription() {
        return "debugging";
    }

    @Override
    public String getSyntax() {
        return "/insellageradmin debug";
    }

    @Override
    public String getPermission() {
        return Permissions.ADMIN_DEBUG.getLabel();
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return null;
    }

    @Override
    public List<String> getTabCompletion(int index, String[] args) {
        if(index == 0) {
            return List.of("autosave", "saveall");
        }
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {

        if(args[0].isEmpty()) {
            sender.sendMessage("§eVerwendung: /iv debug autosave §7|§e /iv debug saveall");
            return;
        }

        ConfigManager cm = IslandVault.getConfigManager();
        switch (args[0].toLowerCase()) {

            // ---------------------------------
            // 📊 AUTOSAVE STATUS
            // ---------------------------------
            case "autosave" -> {
                sender.sendMessage("§6--- Autosave Debug ---");

                if (cm.getDirtyFlags().isEmpty()) {
                    sender.sendMessage("§7Keine dirty Configs.");
                } else {
                    sender.sendMessage("§eDirty Dateien:");
                    cm.getDirtyFlags().forEach((name, dirty) -> {
                        if (dirty) sender.sendMessage("  §a✔ §f" + name);
                    });
                }

                if (cm.getAutosaveTasks().isEmpty()) {
                    sender.sendMessage("§7Keine laufenden Autosave-Tasks.");
                } else {
                    sender.sendMessage("§eLaufende Autosave-Tasks:");
                    cm.getAutosaveTasks().forEach((name, task) ->
                            sender.sendMessage("  §b⏳ §f" + name)
                    );
                }

                if (!cm.getChangeHistory().isEmpty()) {
                    sender.sendMessage("§eÄnderungshäufigkeit (letzte "
                            + cm.getChangeWindowSeconds() + " s):");
                    for (Map.Entry<String, java.util.List<Long>> entry : cm.getChangeHistory().entrySet()) {
                        int recent = cm.getRecentChangeCount(entry.getKey());
                        if (recent > 0) {
                            String color = recent > 50 ? "§c" : (recent > 20 ? "§6" : "§a");
                            sender.sendMessage("  " + color + entry.getKey() + "§7 → " + recent + " Änderungen");
                        }
                    }
                }

                sender.sendMessage("§6--- Ende ---");
            }

            // ---------------------------------
            // 💾 SAVEALL SUBCOMMAND
            // ---------------------------------
            case "saveall" -> {
                int saved = 0;
                for (Map.Entry<String, Boolean> entry : cm.getDirtyFlags().entrySet()) {
                    if (Boolean.TRUE.equals(entry.getValue())) {
                        cm.saveConfig(entry.getKey());
                        saved++;
                    }
                }
                sender.sendMessage("§a" + saved + " Datei" + (saved == 1 ? "" : "en") + " gespeichert.");
            }

            default -> {
                sender.sendMessage("§eUnbekannter Debug-Befehl. Verwendung:");
                sender.sendMessage("§7/insellageradmin debug autosave §8- zeigt Autosave-Status");
                sender.sendMessage("§7/insellageradmin debug saveall §8- speichert alle geänderten Configs sofort");
            }
        }
    }
}
