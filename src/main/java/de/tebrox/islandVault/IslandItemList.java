package de.tebrox.islandVault;// IslandItemList.java
import de.tebrox.islandVault.Manager.ItemGroupManager;
import de.tebrox.islandVault.Utils.ItemStackKey;
import de.tebrox.islandVault.Utils.LuckPermsUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class IslandItemList {

    private final List<ItemPermissionRule> allowedRules = new ArrayList<>();
    private volatile boolean permissionsLoaded = false;

    public boolean arePermissionsLoaded() {
        return permissionsLoaded;
    }

    /**
     * Lädt die Permissions asynchron und füllt allowedRules mit erlaubten Items.
     */
    public CompletableFuture<Void> updatePermissionsAsync(UUID ownerUUID) {
        allowedRules.clear();

        PermissionItemRegistry registry = IslandVault.getPermissionItemRegistry();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Einzelrechte prüfen
        for (Map.Entry<String, ItemPermissionRule> entry : registry.getWhitelistedItems().entrySet()) {
            String itemId = entry.getKey();

            CompletableFuture<Void> future = LuckPermsUtils.hasPermissionForItemAsync(ownerUUID, itemId)
                    .thenAccept(hasPerm -> {
                        if (hasPerm) {
                            synchronized (allowedRules) {
                                allowedRules.add(entry.getValue());
                            }
                        }
                    });
            futures.add(future);
        }

        // Gruppenrechte prüfen
        for (Map.Entry<String, List<String>> groupEntry : ItemGroupManager.getPermissionGroups().entrySet()) {
            String groupName = groupEntry.getKey();

            CompletableFuture<Void> future = LuckPermsUtils.hasPermissionForGroupAsync(ownerUUID, groupName)
                    .thenAccept(hasPerm -> {
                        if (hasPerm) {
                            for (String itemId : groupEntry.getValue()) {
                                registry.getById(itemId.toLowerCase(Locale.ROOT)).ifPresent(rule -> {
                                    synchronized (allowedRules) {
                                        if (!allowedRules.contains(rule)) {
                                            allowedRules.add(rule);
                                        }
                                    }
                                });
                            }
                        }
                    });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> permissionsLoaded = true);
    }

    public List<ItemPermissionRule> getAllowedRules() {
        System.out.println("Size: " + allowedRules.size());
        return allowedRules;
    }

    public List<ItemStack> getAllowedItemStacks() {
        return getAllowedRules().stream()
                .map(rule -> rule.getKey().toItemStack())
                .toList();
    }

    public boolean isItemAllowed(ItemStackKey key) {
        synchronized (allowedRules) {
            return allowedRules.stream().anyMatch(rule -> rule.getKey().equals(key));
        }
    }

    public int size() {
        synchronized (allowedRules) {
            return allowedRules.size();
        }
    }
}
