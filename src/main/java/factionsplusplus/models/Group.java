/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import factionsplusplus.constants.GroupRole;
import factionsplusplus.jsonadapters.GroupMemberAdapter;

/**
 * @author Daniel McCoy Stephenson
 */
public class Group {
    private final List<UUID> invited = new ArrayList<>();
    @Expose
    protected String name = null;
    @Expose
    protected String description = null;
    @Expose
    protected UUID owner = UUID.randomUUID();
    @Expose
    protected List<UUID> officers = new ArrayList<>();
    @Expose
    @JsonAdapter(GroupMemberAdapter.class)
    @SerializedName("members")
    protected HashMap<UUID, GroupMember> members = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        name = newName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newDesc) {
        description = newDesc;
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
        members.put(playerUUID, new GroupMember(playerUUID));
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
        members.put(playerUUID, member);
    }

    public GroupMember getMember(UUID playerUUID) {
        return this.members.get(playerUUID);
    }

    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
    }

    public boolean isMember(UUID playerUUID) {
        return this.members.containsKey(playerUUID);
    }

    public HashMap<UUID, GroupMember> getMembers() {
        return this.members;
    }

    public HashMap<UUID, GroupMember> getMembers(GroupRole role) {
        return (HashMap<UUID, GroupMember>) this.members.entrySet()
            .stream()
            .filter(k -> k.getValue().hasRole(role))
            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    // public String getMemberListSeparatedByCommas() {
    //     String players = "";
    //     for (GroupMember member : this.getMembers()) {
    //         UUID uuid = member.getId();
    //         UUIDChecker uuidChecker = new UUIDChecker();
    //         String playerName = uuidChecker.findPlayerNameBasedOnUUID(uuid);
    //         players += playerName + ", ";
    //     }
    //     if (players.length() > 0) {
    //         return players.substring(0, players.length() - 2);
    //     }
    //     return "";
    // }

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

    public int getNumOfficers() {
        return (int) this.getMembers()
            .values()
            .stream()
            .filter(member -> member.hasRole(GroupRole.Officer))
            .count();
    }

    public List<GroupMember> getOfficerList() {
        return this.getMembers()
            .values()
            .stream()
            .filter(member -> member.hasRole(GroupRole.Officer))
            .toList();
    }

    public int getPopulation() {
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