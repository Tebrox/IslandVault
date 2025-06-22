package de.tebrox.islandVault.Adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;

/**
 * A custom Gson {@link TypeAdapter} for serializing and deserializing {@link Location} objects.
 * <p>
 * This adapter stores only the block coordinates (x, y, z) and the world name.
 * It omits pitch and yaw, and only supports locations with a non-null world.
 */
public class LocationAdapter extends TypeAdapter<Location> {

    /**
     * Writes a {@link Location} object to JSON.
     * The output format is:
     * <pre>
     * {
     *   "world": "world_name",
     *   "x": 100,
     *   "y": 64,
     *   "z": 200
     * }
     * </pre>
     *
     * @param out   the JSON writer
     * @param value the Location to write, or null
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(JsonWriter out, Location value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        out.name("world").value(value.getWorld().getName());
        out.name("x").value(value.getBlockX());
        out.name("y").value(value.getBlockY());
        out.name("z").value(value.getBlockZ());
        out.endObject();
    }

    /**
     * Reads a {@link Location} object from JSON.
     * Expects the JSON object to contain the world name and block coordinates.
     *
     * @param in the JSON reader
     * @return the deserialized Location, or null if the world does not exist
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Location read(JsonReader in) throws IOException {
        String world = null;
        int x = 0, y = 0, z = 0;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "world": world = in.nextString(); break;
                case "x": x = in.nextInt(); break;
                case "y": y = in.nextInt(); break;
                case "z": z = in.nextInt(); break;
            }
        }
        in.endObject();

        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z);
    }
}