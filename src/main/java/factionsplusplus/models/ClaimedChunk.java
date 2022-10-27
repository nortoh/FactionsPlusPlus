package factionsplusplus.models;

import com.google.gson.annotations.Expose;

import java.util.UUID;

import org.bukkit.Chunk;
import org.jdbi.v3.core.mapper.Nested;

/**
 * @author Daniel McCoy Stephenson
 */
public class ClaimedChunk extends Territory {
    @Expose
    @Nested
    private LocationData chunk;

    public ClaimedChunk() { }
    
    public ClaimedChunk(Chunk initialChunk, UUID factionUUID) {
        this.chunk = new LocationData(initialChunk);
        this.setHolder(factionUUID);
    }

    public Chunk getChunk() {
        return this.chunk.getChunk();
    }

    public void setChunk(Chunk chunk) {
        this.chunk = new LocationData(chunk);
    }

    public int[] getCoordinates() {
        int[] coordinates = new int[2];
        coordinates[0] = this.chunk.getX();
        coordinates[1] = this.chunk.getZ();
        return coordinates;
    }

    public UUID getWorldUUID() {
        return this.chunk.getWorld();
    }

    public int getX() {
        return this.chunk.getX();
    }

    public int getZ() {
        return this.chunk.getZ();
    }

    public String getWorldName() {
        return this.chunk.getChunk().getWorld().getName();
    }
}