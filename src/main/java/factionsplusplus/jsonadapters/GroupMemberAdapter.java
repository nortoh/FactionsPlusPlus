package factionsplusplus.jsonadapters;

import com.google.gson.JsonSerializer;

import factionsplusplus.constants.GroupRole;
import factionsplusplus.models.GroupMember;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.UUID;

public class GroupMemberAdapter implements JsonSerializer<ArrayList<GroupMember>>, JsonDeserializer<ArrayList<GroupMember>> {
    @Override
    public JsonElement serialize(ArrayList<GroupMember> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray result = new JsonArray();

        for (GroupMember member : src) {
            JsonObject data = new JsonObject();
            data.addProperty("uuid", member.getId().toString());

            JsonArray roles = new JsonArray();
            for (GroupRole role : member.getRoles()) {
                roles.add(role.name());
            }

            data.add("roles", roles);
            result.add(data);
        }
        return result;
    }

    @Override
    public ArrayList<GroupMember> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ArrayList<GroupMember> data = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray()) {
            JsonObject object = (JsonObject) element;

            GroupMember member = new GroupMember(UUID.fromString(object.get("uuid").getAsString()));
            JsonArray roles = object.get("roles").getAsJsonArray();

            for (int i = 0; i < roles.size(); i++) {
                member.addRole(GroupRole.valueOf(roles.get(i).getAsString()));
            }

            data.add(member);
        }

        return data;
    }
}