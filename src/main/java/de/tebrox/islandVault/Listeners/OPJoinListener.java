package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.Enums.Prefix;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Update.GitHubUpdateChecker;
import de.tebrox.islandVault.Update.VersionComparator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

import java.util.logging.Level;

public class OPJoinListener implements Listener {
    private final IslandVault plugin;

    public OPJoinListener(IslandVault plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String author = plugin.getDescription().getAuthors().get(0);
        String pluginName = plugin.getDescription().getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            new GitHubUpdateChecker(author, pluginName).getLatestRelease(latest -> {
                String current = plugin.getDescription().getVersion();
                int compare = VersionComparator.compareVersions(current, latest);

                if (compare < 0) {
                    plugin.getLogger().log(Level.WARNING, "Eine neue Version ist verfügbar: " + latest);
                    plugin.getLogger().log(Level.WARNING, "Aktuell verwendete Version: " + current);

                    String releaseUrl = "https://github.com/" + author + "/" + pluginName + "/releases/latest";

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (!player.isOp()) continue;

                            player.sendMessage(ChatColor.GREEN + Prefix.MAIN.getLabel() + " " + ChatColor.GOLD + "Es ist eine neue Version verfügbar: " + ChatColor.YELLOW + latest);
                            player.sendMessage(ChatColor.GREEN + Prefix.MAIN.getLabel() + " " + ChatColor.GOLD + "Aktuelle Version: " + ChatColor.YELLOW + current);

                            TextComponent prefix = new TextComponent(ChatColor.GREEN + Prefix.MAIN.getLabel() + " " + ChatColor.AQUA + "Klicke ");
                            TextComponent clickable = new TextComponent(ChatColor.YELLOW + "[hier]");
                            clickable.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, releaseUrl));
                            clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new Text(ChatColor.WHITE + "Zum GitHub-Release wechseln")));
                            TextComponent suffix = new TextComponent(ChatColor.AQUA + ", um das Update herunterzuladen.");

                            TextComponent fullMessage = new TextComponent();
                            fullMessage.addExtra(prefix);
                            fullMessage.addExtra(clickable);
                            fullMessage.addExtra(suffix);

                            player.spigot().sendMessage(fullMessage);
                        }
                    });
                }
            });
        });
    }
}
