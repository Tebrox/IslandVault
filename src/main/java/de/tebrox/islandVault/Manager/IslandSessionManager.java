package de.tebrox.islandVault.Manager;

import com.google.common.cache.*;
import de.tebrox.islandVault.IslandItemList;
import de.tebrox.islandVault.IslandSession;
import de.tebrox.islandVault.Manager.VaultManager;
import de.tebrox.islandVault.VaultData;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;
import java.util.concurrent.*;

public class IslandSessionManager {

    private final Plugin plugin;
    private final VaultManager vaultManager;

    // Spieler → Insel-ID
    private final Map<UUID, String> playerToIsland = new ConcurrentHashMap<>();

    // Insel-ID → IslandSession (LRU + TTL)
    private final Cache<String, IslandSession> sessionCache;

    public IslandSessionManager(Plugin plugin, VaultManager vaultManager) {
        this.plugin = plugin;
        this.vaultManager = vaultManager;

        this.sessionCache = CacheBuilder.newBuilder()
                .maximumSize(100) // Maximal 100 Inseln gleichzeitig im RAM
                .expireAfterAccess(15, TimeUnit.MINUTES) // Optional: Inaktive Inseln nach 15 min entladen
                .removalListener(this::onEviction)
                .build();
    }

    /**
     * Aufgerufen, wenn eine Insel aus dem Cache entfernt wird
     */
    private void onEviction(RemovalNotification<String, IslandSession> notification) {
        IslandSession session = notification.getValue();
        if (session == null) return;

        // Nur entladen, wenn keine Spieler mehr online sind
        if (session.getPlayers().isEmpty()) {
            vaultManager.unloadVault(notification.getKey(), session.getIsland().getOwner());
            plugin.getLogger().info("Vault für Insel " + notification.getKey() + " automatisch entladen.");
        }
    }

    /**
     * Spieler betritt eine Insel. Löst ggf. das Laden des Vaults und der Itemliste aus.
     */
    public void playerEnteredIsland(UUID playerUUID, Island island) {
        String islandId = island.getUniqueId();
        playerToIsland.put(playerUUID, islandId);

        IslandSession existing = sessionCache.getIfPresent(islandId);
        if (existing != null) {
            existing.getPlayers().add(playerUUID);
            return;
        }

        // Asynchron laden
        vaultManager.getVaultAsync(islandId, island.getOwner()).thenAccept(vault -> {
            if (vault == null) {
                plugin.getLogger().warning("Vault konnte für Insel " + islandId + " nicht geladen werden.");
                return;
            }

            IslandSession session = new IslandSession(island, vault);
            session.getPlayers().add(playerUUID);
            sessionCache.put(islandId, session);

            // Itemliste laden
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                IslandItemList list = new IslandItemList();
                list.updatePermissionsAsync(island.getOwner());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    session.setItemList(list);
                    plugin.getLogger().info("Itemliste für Insel " + islandId + " geladen.");
                });
            });
        });
    }

    /**
     * Spieler verlässt die Insel.
     * Wenn niemand mehr da ist, wird entladen (ggf. via Guava-Cache später).
     */
    public void playerLeftIsland(UUID playerUUID) {
        String islandId = playerToIsland.remove(playerUUID);
        if (islandId == null) return;

        IslandSession session = sessionCache.getIfPresent(islandId);
        if (session == null) return;

        session.getPlayers().remove(playerUUID);

        // Wenn keine Spieler mehr da, wird der Vault bei Auslagerung gespeichert
        // → Kein explizites Entladen hier nötig
    }

    public Optional<IslandSession> getSession(String islandId) {
        return Optional.ofNullable(sessionCache.getIfPresent(islandId));
    }

    public Optional<VaultData> getVaultData(String islandId) {
        return getSession(islandId).map(IslandSession::getVaultData);
    }

    public Optional<IslandItemList> getItemList(String islandId) {
        return getSession(islandId).map(IslandSession::getItemList);
    }

    public Optional<String> getIslandIdByPlayer(UUID playerUUID) {
        return Optional.ofNullable(playerToIsland.get(playerUUID));
    }

    public Optional<IslandSession> getSessionByPlayer(UUID playerUUID) {
        return getIslandIdByPlayer(playerUUID).flatMap(this::getSession);
    }

    public Collection<IslandSession> getAllSessions() {
        return sessionCache.asMap().values();
    }

    public boolean isIslandLoaded(String islandId) {
        return sessionCache.getIfPresent(islandId) != null;
    }

    public void unloadAll() {
        for (IslandSession session : sessionCache.asMap().values()) {
            vaultManager.unloadVault(session.getIsland().getUniqueId(), session.getIsland().getOwner());
        }
        sessionCache.invalidateAll();
        playerToIsland.clear();
    }
}
