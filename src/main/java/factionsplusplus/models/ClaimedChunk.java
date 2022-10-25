package factionsplusplus.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

import java.util.UUID;

import org.bukkit.Chunk;

import factionsplusplus.jsonadapters.ChunkAdapter;


/**
 * @author Daniel McCoy Stephenson
 */
public class ClaimedChunk extends Territory {
    @Expose
    @JsonAdapter(ChunkAdapter.class)
    private Chunk chunk;

    public ClaimedChunk(Chunk initialChunk) {
        this.chunk = initialChunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public int[] getCoordinates() {
        int[] coordinates = new int[2];
        coordinates[0] = chunk.getX();
        coordinates[1] = chunk.getZ();
        return coordinates;
    }

    public UUID getWorldUUID() {
        return this.chunk.getWorld().getUID();
    }

    public String getWorldName() {
        return this.chunk.getWorld().getName();
    }
}