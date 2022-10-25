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
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;

import com.google.gson.annotations.Expose;

import factionsplusplus.constants.GroupRole;

import factionsplusplus.models.interfaces.Identifiable;

/**
 * @author Daniel McCoy Stephenson
 */
public class Group implements Identifiable {
    private final List<UUID> invited = new ArrayList<>();
    @Expose
    protected UUID uuid = UUID.randomUUID();
    @Expose
    protected String name = null;
    @Expose
    protected String description = null;
    @Expose
    protected HashMap<UUID, GroupMember> members = new HashMap<>();

    @Override
    public UUID getUUID() {
        return this.uuid;
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
        GroupMember member = new GroupMember(playerUUID);
        member.addRole(GroupRole.Member);
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
        GroupMember member = new GroupMember(playerUUID);
        member.addRole(role);
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

    public HashMap<UUID, GroupMember> getMembers() {
        return this.members;
    }

    public HashMap<UUID, GroupMember> getMembers(GroupRole role) {
        return (HashMap<UUID, GroupMember>) this.members.entrySet()
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