/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;

import factionsplusplus.constants.GroupRole;

import factionsplusplus.models.interfaces.Identifiable;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * @author Daniel McCoy Stephenson
 */
public class Group implements Identifiable {
    private final List<UUID> invited = Collections.synchronizedList(new ArrayList<>());
    @ColumnName("id")
    protected UUID uuid = UUID.randomUUID();
    protected String name = null;
    protected String description = null;
    protected Map<UUID, GroupMember> members = new ConcurrentHashMap<>();

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String newDesc) {
        this.description = newDesc;
    }

    public boolean isOwner(UUID playerUuid) {
        return this.getMember(playerUuid).hasRole(GroupRole.Owner);
    }

    public void setMembers(Map<UUID, GroupMember> members) {
        this.members = members;
    }

    public GroupMember getOwner() {
        return this.members
            .values()
            .stream()
            .filter(member -> member.hasRole(GroupRole.Owner))
            .findFirst()
            .orElse(null);
    }

    public void setOwner(UUID playerUuid) {
        if (! this.isMember(playerUuid)) this.addMember(playerUuid);

        GroupMember member = this.getMember(playerUuid);

        // If we already have a group owner, remove the current owner.
        GroupMember currentOwner = this.getOwner();
        if (currentOwner != null) currentOwner.removeRole(GroupRole.Owner);

        if (! member.hasRole(GroupRole.Owner)) {
            member.addRole(GroupRole.Owner);
        }
    }

    public boolean hasOwner() {
        return this.getOwner() != null;
    }

    /**
     * Add a new member to the Map with an associated GroupMember object.
     * <p>
     * The playerUUID provided was be stored in a HashMap to allow for fast
     * lookup. This will gurantee that a GroupMember object is assocated with
     * every member in the group.
     *
     * @param playerUUID
     */
    public void addMember(UUID playerUUID) {
        GroupMember member = new GroupMember(playerUUID, GroupRole.Member);
        this.members.put(playerUUID, member);
    }

    /**
     * Add a new member to the Map with an associated GroupMember object.
     * <p>
     * The playerUUID provided was be stored in a HashMap to allow for fast
     * lookup. This will gurantee that a GroupMember object is assocated with
     * every member in the group. The provided role will be stored in an
     * ArrayList of roles.
     *
     * @param playerUUID
     * @param role
     */
    public void addMember(UUID playerUUID, GroupRole role) {
        GroupMember member = new GroupMember(playerUUID, role);
        this.members.put(playerUUID, member);
    }

    public GroupMember getMember(UUID playerUUID) {
        return this.members.get(playerUUID);
    }

    public void removeMember(UUID playerUUID) {
        this.members.remove(playerUUID);
    }

    public boolean isMember(UUID playerUUID) {
        return this.members.containsKey(playerUUID);
    }

    public boolean isMember(OfflinePlayer player) {
        return this.isMember(player.getUniqueId());
    }

    public Map<UUID, GroupMember> getMembers() {
        return this.members;
    }

    public Map<UUID, GroupMember> getMembers(GroupRole role) {
        return (Map<UUID, GroupMember>) this.members.entrySet()
            .stream()
            .filter(k -> k.getValue().isRole(role))
            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    public boolean addOfficer(UUID playerUuid) {
        if (! this.isMember(playerUuid)) this.addMember(playerUuid);

        GroupMember member = this.getMember(playerUuid);
        if (! member.hasRole(GroupRole.Officer)) {
            member.addRole(GroupRole.Officer);
            return true;
        }

        return false;
    }

    public boolean removeOfficer(UUID playerUuid) {
        if (! this.isMember(playerUuid)) return false;

        GroupMember member = this.getMember(playerUuid);
        if (member.hasRole(GroupRole.Officer)) {
            member.removeRole(GroupRole.Officer);
            return true;
        }

        return false;
    }

    public boolean isOfficer(UUID playerUuid) {
        if (! this.isMember(playerUuid)) return false;

        return this.getMember(playerUuid).hasRole(GroupRole.Officer);
    }

    public int getOfficerCount() {
        return this.getMembers(GroupRole.Officer).size();
    }

    public Collection<GroupMember> getOfficers() {
        return this.getMembers(GroupRole.Officer).values();
    }

    public void setMemberRole(UUID playerUUID, GroupRole role) {
        GroupMember member = this.getMember(playerUUID);
        if (member != null) member.addRole(role);
    }

    public boolean addLaborer(UUID playerUuid) {
        GroupMember member = this.getMember(playerUuid);
        if (! member.hasRole(GroupRole.Laborer)) {
            member.addRole(GroupRole.Laborer);
            return true;
        }

        return false;
    }

    public boolean removeLaborer(UUID playerUuid) {
        if (! this.isMember(playerUuid)) return false;

        GroupMember member = this.getMember(playerUuid);
        if (member.hasRole(GroupRole.Laborer)) {
            member.removeRole(GroupRole.Laborer);
            return true;
        }

        return false;
    }

    public boolean isLaborer(UUID playerUUID) {
        if (! this.isMember(playerUUID)) return false;

        return this.getMember(playerUUID).hasRole(GroupRole.Laborer);
    }

    public int getLaborerCount() {
        return this.getMembers(GroupRole.Laborer).size();
    }

    public Collection<GroupMember> getLaborers() {
        return this.getMembers(GroupRole.Laborer).values();
    }

    public int getMemberCount() {
        return this.members.size();
    }

    public void invite(UUID playerUUID) {
        Player player = getServer().getPlayer(playerUUID);
        if (player != null) {
            this.invited.add(playerUUID);
        }
    }

    public void uninvite(UUID playerUUID) {
        this.invited.remove(playerUUID);
    }

    public boolean isInvited(UUID playerUUID) {
        return this.invited.contains(playerUUID);
    }
}