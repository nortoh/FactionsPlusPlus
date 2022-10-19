package factionsplusplus.models;

import com.google.gson.annotations.Expose;

public class LocationData {
    @Expose
    private final int x;
    @Expose
    private final int y;
    @Expose
    private final int z;
    @Expose
    private final String world;

    public LocationData(int x, int y, int z, String world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    public int getX() {
        return this.x;
    }
    
    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public String getWorld() {
        return this.world;
    }
}