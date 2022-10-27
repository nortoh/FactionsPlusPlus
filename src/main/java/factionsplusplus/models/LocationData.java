package factionsplusplus.models;

import com.google.gson.annotations.Expose;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.util.UUID;

public class LocationData {
    @Expose
    @ColumnName("x_position")
    private Integer x = null;
    @Expose
    @ColumnName("y_position")
    private Integer y = null;
    @Expose
    @ColumnName("z_position")
    private Integer z = null;
    @Expose
    @ColumnName("world_id")
    private UUID world;

    public LocationData() { }
    
    public LocationData(Integer x, Integer y, Integer z, UUID world) {
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

    public LocationData(Chunk chunk) {
        this.x = chunk.getX();
        this.z = chunk.getZ();
        this.world = chunk.getWorld().getUID();
    }

    public Integer getX() {
        return this.x;
    }
    
    public Integer getY() {
        return this.y;
    }

    public Integer getZ() {
        return this.z;
    }

    public UUID getWorld() {
        return this.world;
    }

    public Block getBlock() {
        return Bukkit.getWorld(this.world).getBlockAt(this.x, this.y, this.z);
    }

    public Chunk getChunk() {
        return Bukkit.getWorld(this.world).getChunkAt(this.x, this.z);
    }
}