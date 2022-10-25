package factionsplusplus.models;

import java.util.ArrayList;
import java.util.UUID;

import com.google.gson.annotations.Expose;

import factionsplusplus.constants.GroupRole;

public class GroupMember {
    @Expose
    protected final UUID playerUuid;
    @Expose
    protected ArrayList<GroupRole> roles = new ArrayList<GroupRole>();

    public GroupMember(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public UUID getId() {
        return this.playerUuid;
    }

    public void addRole(GroupRole role) {
        if (! this.roles.contains(role)) return;
        this.roles.addAll(GroupRole.getFullRoles(role));
    }

    public void addRoles(GroupRole... roles) {
        for (GroupRole role : roles) {
            if (this.hasRole(role)) continue;
            this.roles.addAll(GroupRole.getFullRoles(role));
            // possibly could contain duplicates, needs testing
            this.roles = (ArrayList<GroupRole>) this.roles.stream().distinct().toList();
        }
    }

    public void removeRole(GroupRole role) {
        if (this.roles.contains(role)) {
            this.roles.removeAll(GroupRole.getFullRoles(role));
        }
    }

    public boolean hasRole(GroupRole role) {
        return this.roles.contains(role);
    }

    public ArrayList<GroupRole> getRoles() {
        return this.roles;
    }
}
