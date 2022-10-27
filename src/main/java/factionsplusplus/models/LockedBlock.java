/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.util.UUID;

import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 */
public class LockedBlock {
    @Expose
    @ColumnName("id")
    private UUID uuid;
    @Expose
    @Nested
    private LocationData block;
    @Expose
    @ColumnName("player_id")
    private UUID owner = UUID.randomUUID();
    @Expose
    @ColumnName("faction_id")
    private UUID faction = null;
    @ColumnName("allow_allies")
    private boolean allowAllies = false;
    @ColumnName("allow_faction_members")
    private boolean allowFactionMembers = false;
    @Expose
    private AccessList accessList;

    public LockedBlock(
        UUID owner,
        UUID faction,
        int xCoord,
        int yCoord,
        int zCoord,
        UUID worldUUID
    ) {
        this.block = new LocationData(xCoord, yCoord, zCoord, worldUUID);
        this.owner = owner;
        this.faction = faction;
        this.accessList = new AccessList();
        this.accessList.addPlayerToAccessList(owner);
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public UUID getWorld() {
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

    public void addToAccessList(UUID player) {
        this.accessList.addPlayerToAccessList(player);
    }

    public void removeFromAccessList(UUID player) {
        this.accessList.removePlayerFromAccessList(player);
    }

    public boolean hasAccess(UUID player) {
        return this.accessList.playerOnAccessList(player);
    }

    public boolean shouldAllowAllies() {
        return this.allowAllies;
    }

    public boolean shouldAllowFactionMembers() {
        return this.allowFactionMembers;
    }

    public void allowAllies() {
        this.allowAllies = true;
    }

    public void denyAllies() {
        this.allowAllies = false;
    }

    public void allowFactionMembers() {
        this.allowFactionMembers = true;
    }

    public void denyFactionMembers() {
        this.allowFactionMembers = false;
    }

    public AccessList getAccessList() {
        return this.accessList;
    }

    public void setFaction(UUID uuid) {
        this.faction = uuid;
    }

    public UUID getFactionID() {
        return this.faction;
    }
}