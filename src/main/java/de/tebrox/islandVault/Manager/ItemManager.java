package de.tebrox.islandVault.Manager;

import de.tebrox.islandVault.IslandVault;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {
    private final IslandVault plugin;

    private final List<Material> materialList = new ArrayList<>();
    private List<String> item_blacklist;
    private List<String> internalBlacklist;

    public ItemManager(IslandVault plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        item_blacklist = plugin.getConfig().getStringList("blacklist");
        internalBlacklist = new ArrayList<>();
        internalBlacklist.add("shovel");
        internalBlacklist.add("sword");
        internalBlacklist.add("axe");
        internalBlacklist.add("hoe");
        internalBlacklist.add("helmet");
        internalBlacklist.add("chestplate");
        internalBlacklist.add("leggings");
        internalBlacklist.add("boots");
        internalBlacklist.add("bundle");
        internalBlacklist.add("rod");
        internalBlacklist.add("bow");
        internalBlacklist.add("shulker");
        internalBlacklist.add("written_book");
        internalBlacklist.add("command");
        internalBlacklist.add("structure");

        createMaterialList();
    }

    public List<String> getItemBlacklist() {
        return item_blacklist;
    }

    private void createMaterialList() {
        materialList.clear();
        Material[] materials = Material.values();
        for(Material mat : materials) {
            if(!mat.isAir() && !materialIsBlacklisted(mat) && !materialIsInternalBlacklisted(mat) && mat.isItem()) {
                materialList.add(mat);
            }
        }
    }

    public boolean materialIsBlacklisted(Material material) {
        for(String b : getItemBlacklist()) {
            if(material.toString().toLowerCase().contains(b.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean materialIsInternalBlacklisted(Material material) {
        for(String b : internalBlacklist) {
            if(material.toString().toLowerCase().contains(b.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public List<Material> getMaterialList() {
        return materialList;
    }
}
