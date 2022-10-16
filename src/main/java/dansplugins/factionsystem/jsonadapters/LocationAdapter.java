package dansplugins.factionsystem.jsonadapters;

import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import static org.bukkit.Bukkit.getServer;

import java.lang.reflect.Type;

public class LocationAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
    @Override
    public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.addProperty("world", src.getWorld().getName());
        result.addProperty("x", src.getX());
        result.addProperty("y", src.getY());
        result.addProperty("z", src.getZ());
        return result;
    }
    @Override
    public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonObject data = json.getAsJsonObject();
        return new Location(
            getServer().createWorld(new WorldCreator(data.get("world").getAsString())),
            data.get("x").getAsDouble(),
            data.get("y").getAsDouble(),
            data.get("z").getAsDouble()
        );
    }
}