package dansplugins.factionsystem.jsonadapters;

import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;

import java.util.ArrayList;
import java.util.UUID;
import java.lang.reflect.Type;

public class ArrayListUUIDAdapter implements JsonSerializer<ArrayList<UUID>>, JsonDeserializer<ArrayList<UUID>> {
    @Override
    public JsonElement serialize(ArrayList<UUID> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray uuidArray = new JsonArray();
        for (UUID uuid : src) uuidArray.add(uuid.toString());
        return uuidArray;
    }
    @Override
    public ArrayList<UUID> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonArray jsonArray = json.getAsJsonArray();
        ArrayList<UUID> uuidList = new ArrayList<>();
        for (JsonElement uuid : jsonArray) uuidList.add(UUID.fromString(uuid.getAsString()));
        return uuidList;
    }
}