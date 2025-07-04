package de.tebrox.islandVault.Manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.tebrox.islandVault.Adapter.LocationAdapter;
import de.tebrox.islandVault.IslandVault;
import de.tebrox.islandVault.Listeners.RegionListener;
import de.tebrox.islandVault.Region.Region;
import de.tebrox.islandVault.Region.RegionSession;
import de.tebrox.islandVault.Utils.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Manages regions associated with islands, including creation, removal, retrieval,
 * and player editing sessions.
 * <p>
 * Regions are stored per island ID and persisted as JSON on disk.
 * Also manages player sessions for region editing.
 */
public class RegionManager {
    private final Map<String, List<Region>> regionMap = new HashMap<>();
    private final Map<UUID, RegionSession> sessions = new HashMap<>();
    private final File dataFile;
    private final Gson gson;

    /**
     * Constructs a RegionManager with the given plugin instance,
     * loads regions from disk, and registers event listeners.
     *
     * @param plugin the plugin instance
     */
    public RegionManager(Plugin plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "regions.json");
        gson = new GsonBuilder()
                .registerTypeAdapter(Location.class, new LocationAdapter())
                .setPrettyPrinting()
                .create();
        IslandVault.getPlugin().getServer().getPluginManager().registerEvents(new RegionListener(), IslandVault.getPlugin());
        load();
    }

    /**
     * Starts a region editing session for a player.
     *
     * @param playerId the UUID of the player
     * @param session the RegionSession to start
     */
    public void startSession(UUID playerId, RegionSession session) {
        sessions.put(playerId, session);
    }

    /**
     * Ends a region editing session for a player and stops associated particle effects.
     *
     * @param playerId the UUID of the player
     */
    public void endSession(UUID playerId) {
        sessions.remove(playerId);

        IslandVault.getParticleManager().stop(Bukkit.getPlayer(playerId), "regionBox");
    }

    /**
     * Retrieves the current region editing session of a player.
     *
     * @param playerId the UUID of the player
     * @return the RegionSession or null if none exists
     */
    public RegionSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    /**
     * Checks whether a player is currently in a region editing session.
     *
     * @param playerId the UUID of the player
     * @return true if the player is in a session, false otherwise
     */
    public boolean isPlayerInSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    /**
     * Adds or replaces a region for the specified island.
     * If a region with the same name exists, it is replaced.
     *
     * @param islandId the ID of the island
     * @param region the Region to add
     */
    public void addRegion(String islandId, Region region) {
        regionMap.computeIfAbsent(islandId, k -> new ArrayList<>()).removeIf(r -> r.getName().equalsIgnoreCase(region.getName()));
        regionMap.get(islandId).add(region);
        save();
    }

    /**
     * Removes a region by name from the specified island.
     *
     * @param islandId the ID of the island
     * @param name the name of the region to remove
     */
    public void removeRegion(String islandId, String name) {
        List<Region> regions = regionMap.get(islandId);
        if (regions != null) {
            regions.removeIf(r -> r.getName().equalsIgnoreCase(name));
            save();
        }
    }

    /**
     * Checks if a region with the given name exists for the specified island.
     *
     * @param islandId the ID of the island
     * @param name the name of the region
     * @return true if exists, false otherwise
     */
    public boolean regionExists(String islandId, String name) {
        return regionMap.getOrDefault(islandId, Collections.emptyList()).stream().anyMatch(r -> r.getName().equalsIgnoreCase(name));
    }

    /**
     * Gets a region by name for the specified island.
     *
     * @param islandId the ID of the island
     * @param name the name of the region
     * @return the Region or null if not found
     */
    public Region getRegion(String islandId, String name) {
        return regionMap.getOrDefault(islandId, Collections.emptyList()).stream().filter(r -> r.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /**
     * Checks if the given location is inside any region of the specified island.
     *
     * @param islandId the ID of the island
     * @param location the Location to check
     * @return true if inside any region, false otherwise
     */
    public boolean isInAnyRegion(String islandId, Location location) {
        return regionMap.getOrDefault(islandId, Collections.emptyList()).stream().anyMatch(r -> r.contains(location));
    }

    /**
     * Returns a list of region names for the specified island.
     *
     * @param islandId the ID of the island
     * @return list of region names
     */
    public List<String> getRegionNames(String islandId) {
        return regionMap.getOrDefault(islandId, Collections.emptyList())
                .stream()
                .map(Region::getName)
                .toList();
    }

    /**
     * Retrieves the island UUID that the player belongs to.
     *
     * @param player the player
     * @return the island UUID as a String
     */
    public String getIslandUUID(Player player) {

        return IslandTracker.getPlayerIsland(player.getUniqueId()).getUniqueId();
    }

    /**
     * Gets all regions for the specified island.
     *
     * @param islandId the ID of the island
     * @return list of regions
     */
    public List<Region> getRegions(String islandId) {
        return regionMap.getOrDefault(islandId, Collections.emptyList());
    }

    /**
     * Saves all regions to the JSON data file.
     */
    public void save() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(regionMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads all regions from the JSON data file.
     */
    public void load() {
        if (!dataFile.exists() || !dataFile.isFile()) return;

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, List<Region>>>() {}.getType();
            Map<String, List<Region>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                regionMap.clear();  // Alte Daten entfernen, um Überschneidungen zu vermeiden
                regionMap.putAll(loaded);
            }
        } catch (Exception e) {
            PluginLogger.error("Fehler beim Laden der Regionen: " + e.getMessage());
        }
    }
}