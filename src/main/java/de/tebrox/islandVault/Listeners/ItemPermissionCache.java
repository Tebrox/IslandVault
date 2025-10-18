package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Manager.ItemManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.event.user.track.UserTrackEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Verwaltet für jeden Spieler den Cache der freigeschalteten Item-Permissions.
 * - Baut Cache beim Join
 * - Aktualisiert bei LuckPerms-Änderungen
 * - Entfernt Cache beim Quit
 */
public class ItemPermissionCache implements Listener {

    private final IslandVault plugin;
    private final LuckPerms luckPerms;
    private final ItemManager itemManager;

    private final Map<UUID, Set<String>> unlockedPermissionKeys = new ConcurrentHashMap<>();

    public ItemPermissionCache(IslandVault plugin, LuckPerms luckPerms, ItemManager itemManager) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.itemManager = itemManager;

        // LuckPerms Event-Listener registrieren
        luckPerms.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        luckPerms.getEventBus().subscribe(plugin, UserTrackEvent.class, this::onTrackChange);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ============================================================
    // ============   Join / Quit Event Handling   ================
    // ============================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> loadPlayerCache(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        unlockedPermissionKeys.remove(event.getPlayer().getUniqueId());
    }

    // ============================================================
    // ================   Cache Logik   ============================
    // ============================================================

    private void loadPlayerCache(Player player) {
        if (player == null || !player.isOnline()) return;

        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        if (user == null) return;

        Set<String> perms = new HashSet<>();
        for (ItemStack item : itemManager.getItemList()) {
            String perm = itemManager.buildPermissionKey(item);
            if (user.getCachedData().getPermissionData().checkPermission(perm).asBoolean()) {
                perms.add(perm);
            }
        }

        unlockedPermissionKeys.put(player.getUniqueId(), perms);
        plugin.getLogger().log(Level.FINE,
                "[ItemCache] Spieler " + player.getName() + ": " + perms.size() + " freigeschaltete Items gecacht.");
    }

    // ============================================================
    // ============   LuckPerms Event Handling   ==================
    // ============================================================

    /**
     * Wird aufgerufen, wenn sich die effektiven Rechte eines Spielers ändern
     * (z. B. Gruppenzuordnung, neue Node, Track-Wechsel, etc.).
     */
    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        User user = event.getUser();
        Player player = Bukkit.getPlayer(user.getUniqueId());
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                loadPlayerCache(player);
                plugin.getLogger().log(Level.FINE,
                        "[ItemCache] Cache für " + player.getName() + " aktualisiert (UserDataRecalculateEvent).");
            });
        }
    }

    private void onTrackChange(UserTrackEvent event) {
        UUID uuid = event.getUser().getUniqueId();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                loadPlayerCache(player);
                plugin.getLogger().log(Level.FINE,
                        "[ItemCache] Cache für " + player.getName() + " aktualisiert (TrackChange).");
            });
        }
    }

    // ============================================================
    // ===============   Abfrage-Methoden   ========================
    // ============================================================

    public boolean isUnlocked(Player player, ItemStack item) {
        Set<String> perms = unlockedPermissionKeys.get(player.getUniqueId());
        return perms != null && perms.contains(itemManager.buildPermissionKey(item));
    }

    public List<ItemStack> getUnlockedItems(Player player) {
        Set<String> perms = unlockedPermissionKeys.get(player.getUniqueId());
        if (perms == null || perms.isEmpty()) return Collections.emptyList();

        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : itemManager.getItemList()) {
            String key = itemManager.buildPermissionKey(item);
            if (perms.contains(key)) result.add(item);
        }
        return result;
    }
}