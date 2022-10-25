/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.externalapi;

import factionsplusplus.models.Faction;
import factionsplusplus.models.GroupMember;
import factionsplusplus.models.ConfigurationFlag;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class FPP_Faction {
    private final Faction faction;

    public FPP_Faction(Faction faction) {
        this.faction = faction;
    }

    public UUID getID() {
        return this.faction.getID();
    }

    public String getName() {
        return this.faction.getName();
    }

    public String getDescription() {
        return this.faction.getDescription();
    }

    public String getPrefix() {
        return this.faction.getPrefix();
    }

    public GroupMember getOwner() {
        return this.faction.getOwner();
    }

    public boolean isMember(Player player) {
        return this.faction.isMember(player.getUniqueId());
    }

    public boolean isOfficer(Player player) {
        return this.faction.isOfficer(player.getUniqueId());
    }

    public boolean isAlly(UUID factionUUID) {
        return this.faction.isAlly(factionUUID);
    }

    public boolean isEnemy(UUID factionUUID) {
        return this.faction.isEnemy(factionUUID);
    }

    public List<UUID> getEnemies() {
        return this.faction.getEnemyFactions();
    }

    public List<UUID> getAllies() {
        return this.faction.getAllies();
    }

    public List<GroupMember> getOfficers() {
        return this.faction.getOfficerList();
    }

    public HashMap<UUID, GroupMember> getMembers() {
        return this.faction.getMembers();
    }

    public boolean hasFlag(String flagName) {
        return this.faction.getFlags().containsKey(flagName);
    }

    public ConfigurationFlag getFlag(String flagName) {
        return this.faction.getFlag(flagName);
    }

    /**
     * This should only be used when the external API is not sufficient. It should be noted that the underlying implementation is prone to change.
     *
     * @return The underlying implementation of the faction class.
     */
    @Deprecated
    public Faction getUnderlyingImplementation() {
        return this.faction;
    }
}