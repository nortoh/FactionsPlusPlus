package dansplugins.factionsystem.jsonadapters;

import dansplugins.factionsystem.models.Gate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;

import dansplugins.factionsystem.models.Gate;

import java.util.ArrayList;
import java.util.UUID;
import java.lang.reflect.Type;

public class ArrayListGateAdapter implements JsonSerializer<ArrayList<Gate>>, JsonDeserializer<ArrayList<Gate>> {
    @Override
    public JsonElement serialize(ArrayList<Gate> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray gateArray = new JsonArray();
        for (Gate gate : src) gateArray.add(gate.toJson());
        return gateArray;
    }
    @Override
    public ArrayList<Gate> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        ArrayList<Gate> gates = new ArrayList<>();
        JsonArray gateArray = json.getAsJsonArray();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
        for (JsonElement gateElement : gateArray) {
            gates.add(gson.fromJson(gateElement, Gate.class));
        }
        return gates;
    }
}