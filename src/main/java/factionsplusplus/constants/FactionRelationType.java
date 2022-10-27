package factionsplusplus.constants;

import org.jdbi.v3.core.enums.DatabaseValue;

public enum FactionRelationType {
    @DatabaseValue("1")
    Ally,
    @DatabaseValue("2")
    Vassal,
    @DatabaseValue("3")
    Liege,
    @DatabaseValue("4")
    Enemy;
}