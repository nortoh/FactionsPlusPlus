package dansplugins.factionsystem.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Chunk;

import dansplugins.factionsystem.jsonadapters.ChunkAdapter;

import java.util.HashMap;
import java.util.Map;


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

    public String getWorldName() {
        return this.chunk.getWorld().getName();
    }

    // Tools
    public JsonElement toJsonTree() {
        return new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .serializeNulls()
            .create()
            .toJsonTree(this);
    }
}