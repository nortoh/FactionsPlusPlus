/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 */
public class LockedBlock {
    @Expose
    private final LocationData block;
    @Expose
    private UUID owner = UUID.randomUUID();
    @Expose
    private UUID faction = null;
    @Expose
    private AccessList accessList;

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
        this.accessList = new AccessList();
        this.accessList.addPlayerToAccessList(owner);
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

    public void addToAccessList(UUID player) {
        this.accessList.addPlayerToAccessList(player);
    }

    public void removeFromAccessList(UUID player) {
        this.accessList.removePlayerFromAccessList(player);
    }

    public boolean hasAccess(UUID player) {
        return this.accessList.playerOnAccessList(player);
    }

    public void allowAllies() {
        this.accessList.addAlliesToAccessList();
    }

    public void denyAllies() {
        this.accessList.removeAlliesFromAccessList();
    }

    public void allowFactionMembers() {
        this.accessList.addFactionMembersToAccessList();
    }

    public void denyFactionMembers() {
        this.accessList.removeFactionMembersFromAccessList();
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