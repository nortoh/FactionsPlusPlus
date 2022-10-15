package dansplugins.factionsystem.jsonadapters;

import dansplugins.factionsystem.objects.helper.GateCoord;

import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;

import java.util.UUID;
import java.lang.reflect.Type;

public class GateCoordAdapter implements JsonSerializer<GateCoord>, JsonDeserializer<GateCoord> {
    @Override
    public JsonElement serialize(GateCoord src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString()); 
    }
    @Override
    public GateCoord deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        return GateCoord.fromString(json.getAsString());
    }
}