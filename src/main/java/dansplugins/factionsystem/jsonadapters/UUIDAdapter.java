package dansplugins.factionsystem.jsonadapters;

import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;

import java.util.UUID;
import java.lang.reflect.Type;

public class UUIDAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
    @Override
    public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString()); 
    }
    @Override
    public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        return UUID.fromString(json.getAsString());
    }
}