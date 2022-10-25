package factionsplusplus.models;

import java.util.List;
import java.util.UUID;

import com.google.gson.annotations.Expose;

import factionsplusplus.constants.GroupRole;
import factionsplusplus.models.interfaces.Identifiable;

public class GroupMember implements Identifiable {
    @Expose
    protected final UUID uuid;
    @Expose
    protected int role = GroupRole.Member.getLevel();

    public GroupMember(UUID playerUuid) {
        this.uuid = playerUuid;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public void addRole(GroupRole role) {
        this.role |= role.getLevel();
    }

    public void removeRole(GroupRole role) {
        this.role &= ~role.getLevel();
    }

    public boolean hasRole(GroupRole role) {
        return (this.role & role.getLevel()) == role.getLevel();
    }

    public boolean isRole(GroupRole role) {
        return this.role == role.getLevel();
    }

    public List<GroupRole> getRoles() {
        return GroupRole.getRoleList(this.role);
    }
}
