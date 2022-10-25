package factionsplusplus.constants;

import java.util.ArrayList;

public enum GroupRole {
    Member(null),
    Laborar(Member),
    Officer(Laborar),
    Owner(Officer);

    private GroupRole inherentince;

    GroupRole(GroupRole inherentince) {
        this.inherentince = inherentince;
    }

    public GroupRole getInheritedRole() {
        return this.inherentince;
    }

    public static ArrayList<GroupRole> getFullRoles(GroupRole role) {
        ArrayList<GroupRole> inherited = new ArrayList<>();

        while (role != Member) {
            inherited.add(role);
            role = role.inherentince;
        }

        inherited.add(Member);
        return inherited;
    }
}
