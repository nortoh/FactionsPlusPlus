package factionsplusplus.constants;

import org.jdbi.v3.core.enums.DatabaseValue;

public enum FlagType {
    @DatabaseValue("1")
    Faction,
    @DatabaseValue("2")
    World;
}