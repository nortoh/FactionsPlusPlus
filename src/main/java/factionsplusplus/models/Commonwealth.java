package factionsplusplus.models;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import factionsplusplus.models.interfaces.*;

public class Commonwealth implements Identifiable {

    private final String name = null;
    private final UUID uuid = UUID.randomUUID();
    private final List<UUID> memberNations = new ArrayList<>();

    public String getName() {
        return this.name;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public void addMemberNation(UUID nation) {
        if (! this.memberNations.contains(nation)) this.memberNations.add(nation);
    }

    public void removeMemberNation(UUID nation) {
        this.memberNations.remove(nation);
    }
}