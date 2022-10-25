package factionsplusplus.models;

import com.google.gson.annotations.Expose;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

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

    public LocationData(Block block) {
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
        this.world = block.getWorld().getName();
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

    public Block getBlock() {
        return Bukkit.getWorld(this.world).getBlockAt(x, y, z);
    }
}