package de.tebrox.islandVault.InventoryHolder;

import de.tebrox.islandVault.Items.VaultChest;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class FilterInventoryHolder implements InventoryHolder {

    private final VaultChest vaultChest;
    private final Block block;
    private Inventory inventory;

    public FilterInventoryHolder(VaultChest vaultChest, Block block) {
        this.vaultChest = vaultChest;
        this.block = block;
    }

    public VaultChest getVaultChest() { return vaultChest; }

    public Block getBlock() { return block; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
