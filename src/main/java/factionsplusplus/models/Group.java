/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;

import com.google.gson.annotations.Expose;

import factionsplusplus.constants.GroupRole;

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
    protected HashMap<UUID, GroupRole> roles = new HashMap<>();
    @Expose
    protected ArrayList<GroupMember> members = new ArrayList<>();

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
            .stream()
            .filter(member -> member.hasRole(GroupRole.Owner))
            .findAny()
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
     * @param playerUuid
     */
    public void addMember(UUID playerUuid) {
        GroupMember member = new GroupMember(playerUuid);
        member.addRole(GroupRole.Member);
        members.add(new GroupMember(playerUuid));

    }

    /**
     * Add a new member to the Map with an associated GroupMember object.
     * <p>
     * The playerUUID provided was be stored in a HashMap to allow for fast
     * lookup. This will gurantee that a GroupMember object is assocated with
     * every member in the group. The provided role will be stored in an
     * ArrayList of roles.
     *
     * @param playerUuid
     * @param role
     */
    public void addMember(UUID playerUuid, GroupRole role) {
        GroupMember member = new GroupMember(playerUuid);
        member.addRole(role);
        members.add(member);
    }

    public GroupMember getMember(UUID playerUuid) {
        return this.members
            .stream()
            .filter(member -> member.getId() == playerUuid)
            .findFirst()
            .orElse(null);
    }

    public void removeMember(UUID playerUuid) {
        members.remove(this.getMember(playerUuid));
    }

    public boolean isMember(UUID playerUuid) {
        return this.members
            .stream()
            .filter(member -> member.getId() == playerUuid)
            .count() == 1;
    }

    public ArrayList<GroupMember> getMembers() {
        return this.members;
    }

    public ArrayList<UUID> getMembersUUIDS() {
        return (ArrayList<UUID>) this.members.
            stream()
            .map(member -> member.getId())
            .collect(Collectors.toList());
    }

    public ArrayList<UUID> getMembersUUIDS(GroupRole role) {
        return (ArrayList<UUID>) this.members.stream()
            .filter(member -> member.hasRole(role))
            .map(uuid -> uuid.getId())
            .collect(Collectors.toList());
    }

    public String getMemberListSeparatedByCommas() {
        String players = "";
        for (GroupMember member : this.getMembers()) {
            UUID uuid = member.getId();
            UUIDChecker uuidChecker = new UUIDChecker();
            String playerName = uuidChecker.findPlayerNameBasedOnUUID(uuid);
            players += playerName + ", ";
        }
        if (players.length() > 0) {
            return players.substring(0, players.length() - 2);
        }
        return "";
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

    public int getNumOfficers() {
        return (int) this.getMembers()
            .stream()
            .filter(member -> member.hasRole(GroupRole.Officer))
            .count();
    }

    public List<GroupMember> getOfficerList() {
        return this.getMembers()
            .stream()
            .filter(member -> member.hasRole(GroupRole.Officer))
            .toList();
    }

    public int getPopulation() {
        return this.members.size();
    }

    public void invite(UUID playerUuid) {
        Player player = getServer().getPlayer(playerUuid);
        if (player != null) {
            playerUuid = getServer().getPlayer(playerUuid).getUniqueId();
            this.invited.add(playerUuid);
        }
    }

    public void uninvite(UUID playerUuid) {
        this.invited.remove(playerUuid);
    }

    public boolean isInvited(UUID playerUuid) {
        return this.invited.contains(playerUuid);
    }
}