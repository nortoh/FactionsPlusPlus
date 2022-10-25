package factionsplusplus.constants;

import java.util.ArrayList;
import java.util.List;

public enum GroupRole {
    Member(0x1),
    Laborer(0x2 | GroupRole.Member.getLevel()),
    Officer(0x4 | GroupRole.Laborer.getLevel() | GroupRole.Member.getLevel()),
    Owner(0x8 | GroupRole.Officer.getLevel() | GroupRole.Laborer.getLevel() | GroupRole.Member.getLevel());

    private int level;

    GroupRole(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }

    public static List<GroupRole> getRoleList(int role) {
        ArrayList<GroupRole> roles = new ArrayList<>();

        for (GroupRole potentialRole : GroupRole.values()) if ((role & potentialRole.level) == potentialRole.level) roles.add(potentialRole);

        return roles;
    }
}
