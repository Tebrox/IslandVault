package de.tebrox.islandVault.items;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {
    private final IslandVault plugin;

    private final List<Material> materialList = new ArrayList<>();
    private List<String> item_blacklist;

    public ItemManager(IslandVault plugin) {
        this.plugin = plugin;
        item_blacklist = plugin.getConfig().getStringList("blacklist");

        createMaterialList();
    }

    public List<String> getItemBlacklist() {
        return item_blacklist;
    }

    private void createMaterialList() {
        Material[] materials = Material.values();
        for(Material mat : materials) {
            if(!mat.isAir()) {
                materialList.add(mat);
            }
        }
    }

    public boolean materialIsBlacklisted(Material material) {
        for(String b : getItemBlacklist()) {
            if(material.toString().toLowerCase().contains(b)) {
                return true;
            }
        }
        return false;
    }

    public List<Material> getMaterialList() {
        return materialList;
    }
}
