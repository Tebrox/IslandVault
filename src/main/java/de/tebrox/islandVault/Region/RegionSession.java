package de.tebrox.islandVault.Region;

import org.bukkit.Location;

public class RegionSession {
    private final ActionType action;
    private final String name;
    private Location pos1, pos2;
    private boolean waitingForConfirmation = true;

    public enum ActionType {
        CREATE,
        EDIT
    }

    public RegionSession(String name, ActionType action) {
        this.name = name;
        this.action = action;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos) {
        this.pos1 = pos;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos) {
        this.pos2 = pos;
    }

    public String getName() {
        return name;
    }

    public ActionType getAction() {
        return action;
    }
}