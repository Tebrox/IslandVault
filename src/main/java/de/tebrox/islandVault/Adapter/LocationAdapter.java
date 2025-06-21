package de.tebrox.islandVault.Adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;

public class LocationAdapter extends TypeAdapter<Location> {
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