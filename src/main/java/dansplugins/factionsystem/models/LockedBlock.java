/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dansplugins.factionsystem.jsonadapters.UUIDAdapter;
import dansplugins.factionsystem.jsonadapters.ArrayListUUIDAdapter;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

/**
 * @author Daniel McCoy Stephenson
 */
public class LockedBlock {
    @Expose
    private final LocationData block;
    @Expose
    @JsonAdapter(UUIDAdapter.class)
    private UUID owner = UUID.randomUUID();
    @Expose
    private UUID faction = null;
    @Expose
    @JsonAdapter(ArrayListUUIDAdapter.class)
    private ArrayList<UUID> accessList = new ArrayList<>();

    public LockedBlock(
        UUID owner,
        UUID faction,
        int xCoord,
        int yCoord,
        int zCoord,
        String worldName
    ) {
        this.block = new LocationData(xCoord, yCoord, zCoord, worldName);
        this.owner = owner;
        this.faction = faction;
        this.accessList.add(owner);
    }

    public String getWorld() {
        return this.block.getWorld();
    }

    public int getX() {
        return this.block.getX();
    }

    public int getY() {
        return this.block.getY();
    }

    public int getZ() {
        return this.block.getZ();
    }

    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void addToAccessList(UUID playerName) {
        if (!this.accessList.contains(playerName)) {
            this.accessList.add(playerName);
        }
    }

    public void removeFromAccessList(UUID playerName) {
        this.accessList.remove(playerName);
    }

    public boolean hasAccess(UUID playerName) {
        return this.accessList.contains(playerName);
    }

    public ArrayList<UUID> getAccessList() {
        return this.accessList;
    }

    public void setFaction(UUID uuid) {
        this.faction = uuid;
    }

    public UUID getFactionID() {
        return this.faction;
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