package de.tebrox.islandVault.Region;

import org.bukkit.Location;

public class Region {
    private final String name;
    private Location pos1;
    private Location pos2;

    public Region(String name, Location pos1, Location pos2) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public String getName() { return name; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }

    public void setPos1(Location pos) {
        this.pos1 = pos;
    }

    public void setPos2(Location pos) {
        this.pos2 = pos;
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) return false;

        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}