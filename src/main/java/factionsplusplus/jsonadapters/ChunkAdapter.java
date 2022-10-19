package factionsplusplus.jsonadapters;

import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import static org.bukkit.Bukkit.getServer;

import java.lang.reflect.Type;

public class ChunkAdapter implements JsonSerializer<Chunk>, JsonDeserializer<Chunk> {
    @Override
    public JsonElement serialize(Chunk src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.addProperty("world", src.getWorld().getName());
        result.addProperty("x", src.getX());
        result.addProperty("z", src.getZ());
        return result;
    }
    @Override
    public Chunk deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonObject data = json.getAsJsonObject();
        World chunkWorld = getServer().createWorld(new WorldCreator(data.get("world").getAsString()));
        return chunkWorld.getChunkAt(
            data.get("x").getAsInt(),
            data.get("z").getAsInt()
        );
    }
}