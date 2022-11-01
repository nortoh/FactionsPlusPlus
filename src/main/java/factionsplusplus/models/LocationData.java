package factionsplusplus.models;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import factionsplusplus.data.beans.LocationDataBean;

import java.util.UUID;

public class LocationData {
    @ColumnName("x_position")
    private Double x = null;
    @ColumnName("y_position")
    private Double y = null;
    @ColumnName("z_position")
    private Double z = null;
    @ColumnName("world_id")
    private UUID world;

    public LocationData() { }
    
    public LocationData(Integer x, Integer y, Integer z, UUID world) {
        this.x = Double.valueOf(x);
        this.y = Double.valueOf(y);
        this.z = Double.valueOf(z);
        this.world = world;
    }

    public LocationData(Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.world = location.getWorld().getUID();
    }

    public LocationData(Block block) {
        this.x = Double.valueOf(block.getX());
        this.y = Double.valueOf(block.getY());
        this.z = Double.valueOf(block.getZ());
        this.world = block.getWorld().getUID();
    }

    public LocationData(Chunk chunk) {
        this.x = Double.valueOf(chunk.getX());
        this.z = Double.valueOf(chunk.getZ());
        this.world = chunk.getWorld().getUID();
    }

    public LocationData(LocationDataBean bean) {
        this.x = Double.valueOf(bean.getX());
        this.y = Double.valueOf(bean.getY());
        this.z = Double.valueOf(bean.getZ());
        this.world = bean.getWorld();
    }

    public Integer getX() {
        return this.x != null ? this.x.intValue() : null;
    }
    
    public Integer getY() {
        return this.y != null ? this.y.intValue() : null;
    }

    public Integer getZ() {
        return this.z != null ? this.z.intValue() : null;
    }

    public UUID getWorld() {
        return this.world;
    }

    public Block getBlock() {
        return Bukkit.getWorld(this.world).getBlockAt(this.x.intValue(), this.y.intValue(), this.z.intValue());
    }

    public Chunk getChunk() {
        return Bukkit.getWorld(this.world).getChunkAt(this.x.intValue(), this.z.intValue());
    }

    public Location getLocation() {
        return new Location(
            Bukkit.getWorld(this.world),
            this.x,
            this.y,
            this.z
        );
    }
}