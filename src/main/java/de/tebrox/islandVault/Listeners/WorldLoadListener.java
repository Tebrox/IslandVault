package de.tebrox.islandVault.Listeners;

import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Items.VaultChest;
import de.tebrox.islandVault.Utils.IslandUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class WorldLoadListener implements Listener {

    private final IslandVault plugin;

    public WorldLoadListener(IslandVault plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {

        World world = event.getWorld();
        String worldName = world.getName();
        plugin.getLogger().log(Level.INFO, "Welt " + worldName + " wird geladen");
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection chestsSection = cfg.getConfigurationSection("chests." + worldName);
        if (chestsSection == null) {
            return; // in dieser Welt keine Chests
        }

        for (String key : chestsSection.getKeys(false)) {
            String basePath = "chests." + worldName + "." + key;

            // Location laden
            Location loc = plugin.deserializeLoc(cfg.getString(basePath + ".loc"));
            if (loc == null) {
                plugin.getLogger().warning("Chest " + basePath + " hat ungültige Location.");
                continue;
            }

            // Owner laden
            String ownerStr = cfg.getString(basePath + ".owner");
            if (ownerStr == null) {
                plugin.getLogger().warning("Chest " + basePath + " ohne Owner übersprungen.");
                continue;
            }
            UUID owner = UUID.fromString(ownerStr);

            // Island laden
            String islandId = cfg.getString(basePath + ".islandID");
            Optional<Island> islandOpt = (islandId != null)
                    ? IslandUtils.getIslandManager().getIslandById(islandId)
                    : Optional.empty();

            VaultChest chest = new VaultChest(owner, loc, islandOpt.orElse(null));

            // Filter laden
            ItemStack[] tempFilter = new ItemStack[9];
            ConfigurationSection filterSec = cfg.getConfigurationSection(basePath + ".filter");
            if (filterSec != null) {
                for (int i = 0; i < 9; i++) {
                    ItemStack item = cfg.getItemStack(basePath + ".filter." + i);
                    if (item != null) {
                        tempFilter[i] = item;
                    }
                }
            }
            chest.setSavedFilter(tempFilter);

            boolean inputMode = cfg.getString(basePath + ".mode").equals("input") ? true : false;
            chest.setMode(inputMode);

            // ins Plugin-Mapping
            IslandVault.customChests.put(loc, chest);
            plugin.getLogger().info("Chest geladen: " + loc + " in Welt " + worldName);
        }
    }
}