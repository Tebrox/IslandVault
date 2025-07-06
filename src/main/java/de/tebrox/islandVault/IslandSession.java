package de.tebrox.islandVault;

import de.tebrox.islandVault.IslandItemList;
import de.tebrox.islandVault.VaultData;
import world.bentobox.bentobox.database.objects.Island;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IslandSession {
    private final Island island;
    private final VaultData vaultData;
    private final Set<UUID> players = new HashSet<>();
    private IslandItemList itemList;

    public IslandSession(Island island, VaultData vaultData) {
        this.island = island;
        this.vaultData = vaultData;
    }

    public Island getIsland() {
        return island;
    }

    public VaultData getVaultData() {
        return vaultData;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public IslandItemList getItemList() {
        return itemList;
    }

    public void setItemList(IslandItemList itemList) {
        this.itemList = itemList;
    }
}
