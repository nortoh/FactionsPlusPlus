package factionsplusplus.models;

import com.google.gson.annotations.Expose;
import org.bukkit.block.Block;

import java.util.UUID;

public class LocationData {
    @Expose
    private final int x;
    @Expose
    private final int y;
    @Expose
    private final int z;
    @Expose
    private final UUID world;

    public LocationData(int x, int y, int z, UUID world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    public LocationData(Block block) {
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
        this.world = block.getWorld().getUID();
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

    public UUID getWorld() {
        return this.world;
    }
}