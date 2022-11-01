package factionsplusplus.models;

import java.util.List;
import java.util.UUID;

import factionsplusplus.constants.GroupRole;
import factionsplusplus.models.interfaces.Identifiable;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class GroupMember implements Identifiable {
    @ColumnName("id")
    protected UUID uuid;
    protected int role = GroupRole.Member.getLevel();

    public GroupMember() { }

    public GroupMember(UUID playerUuid) {
        this.uuid = playerUuid;
    }

    public GroupMember(UUID playerUuid, GroupRole role) {
        this.uuid = playerUuid;
        this.role = role.getLevel();
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

    public int getRole() {
        return this.role;
    }

    public List<GroupRole> getRoles() {
        return GroupRole.getRoleList(this.role);
    }
}
