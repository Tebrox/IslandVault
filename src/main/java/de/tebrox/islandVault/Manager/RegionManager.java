package de.tebrox.islandVault.Manager;

import com.google.common.reflect.TypeToken;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Listeners.RegionListener;
import de.tebrox.islandVault.Region.Region;
import de.tebrox.islandVault.Region.RegionSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class RegionManager {
    private final Map<String, List<Region>> regionMap = new HashMap<>();
    private final Map<UUID, RegionSession> sessions = new HashMap<>();
    private final File dataFile;

    public RegionManager(Plugin plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "regions.json");
        IslandVault.getPlugin().getServer().getPluginManager().registerEvents(new RegionListener(), IslandVault.getPlugin());
        load();
    }

    public void startSession(UUID playerId, RegionSession session) {
        sessions.put(playerId, session);
    }

    public void endSession(UUID playerId) {
        sessions.remove(playerId);
    }

    public RegionSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public void addRegion(String islandId, Region region) {
        regionMap.computeIfAbsent(islandId, k -> new ArrayList<>()).removeIf(r -> r.getName().equalsIgnoreCase(region.getName()));
        regionMap.get(islandId).add(region);
        save();
    }

    public void removeRegion(String islandId, String name) {
        List<Region> regions = regionMap.get(islandId);
        if (regions != null) {
            regions.removeIf(r -> r.getName().equalsIgnoreCase(name));
            save();
        }
    }

    public boolean regionExists(String islandId, String name) {
        return regionMap.getOrDefault(islandId, Collections.emptyList()).stream().anyMatch(r -> r.getName().equalsIgnoreCase(name));
    }

    public Region getRegion(String islandId, String name) {
        return regionMap.getOrDefault(islandId, Collections.emptyList()).stream().filter(r -> r.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public boolean isInAnyRegion(String islandId, Location location) {
        return regionMap.getOrDefault(islandId, Collections.emptyList()).stream().anyMatch(r -> r.contains(location));
    }

    public List<String> getRegionNames(String islandId) {
        return regionMap.getOrDefault(islandId, Collections.emptyList())
                .stream()
                .map(Region::getName)
                .toList();
    }

    public String getIslandUUID(Player player) {

        return IslandTracker.getPlayerIsland(player.getUniqueId()).getUniqueId();
    }

    public List<Region> getRegions(String islandId) {
        return regionMap.getOrDefault(islandId, Collections.emptyList());
    }

    public void save() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            IslandVault.getGson().toJson(regionMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!dataFile.exists()) return;
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, List<Region>>>() {}.getType();
            Map<String, List<Region>> loaded = IslandVault.getGson().fromJson(reader, type);
            if (loaded != null) regionMap.putAll(loaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}