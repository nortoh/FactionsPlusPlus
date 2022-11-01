/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import factionsplusplus.data.repositories.LockedBlockRepository;

/**
 * @author Daniel McCoy Stephenson
 */
public class LockedBlock {
    @ColumnName("id")
    private UUID uuid = UUID.randomUUID();
    @Nested
    private LocationData block;
    @ColumnName("player_id")
    private UUID owner;
    @ColumnName("faction_id")
    private UUID faction = null;
    @ColumnName("allow_allies")
    private boolean allowAllies = false;
    @ColumnName("allow_faction_members")
    private boolean allowFactionMembers = false;
    private List<UUID> accessList = new ArrayList<>();

    private final LockedBlockRepository lockedBlockRepository;

    @AssistedInject
    public LockedBlock(
        @Assisted("owner") UUID owner,
        @Assisted("faction") UUID faction,
        @Assisted("x") int xCoord,
        @Assisted("y") int yCoord,
        @Assisted("z") int zCoord,
        @Assisted("world") UUID worldUUID,
        LockedBlockRepository lockedBlockRepository
    ) {
        this.block = new LocationData(xCoord, yCoord, zCoord, worldUUID);
        this.owner = owner;
        this.faction = faction;
        this.accessList.add(owner);
        this.lockedBlockRepository = lockedBlockRepository;
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

    public void setAccessList(List<UUID> list) {
        this.accessList = list;
    }

    public void addToAccessList(UUID player) {
        if (! this.accessList.contains(player)) {
            this.lockedBlockRepository.persistPlayerAccess(this, player);
            this.accessList.add(player);
        }
    }

    public void removeFromAccessList(UUID player) {
        if (this.accessList.contains(player)) {
            this.lockedBlockRepository.deletePlayerAccess(this, player);
            this.accessList.remove(player);
        }
    }

    public boolean hasAccess(UUID player) {
        return this.accessList.contains(player);
    }

    public boolean shouldAllowAllies() {
        return this.allowAllies;
    }

    public boolean shouldAllowFactionMembers() {
        return this.allowFactionMembers;
    }

    public void allowAllies() {
        this.allowAllies = true;
        this.lockedBlockRepository.persist(this);
    }

    public void denyAllies() {
        this.allowAllies = false;
        this.lockedBlockRepository.persist(this);
    }

    public void allowFactionMembers() {
        this.allowFactionMembers = true;
        this.lockedBlockRepository.persist(this);
    }

    public void denyFactionMembers() {
        this.allowFactionMembers = false;
        this.lockedBlockRepository.persist(this);
    }

    public List<UUID> getAccessList() {
        return this.accessList;
    }

    public void setFaction(UUID uuid) {
        this.faction = uuid;
    }

    public UUID getFaction() {
        return this.faction;
    }
}