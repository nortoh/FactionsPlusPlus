package factionsplusplus.constants;

import org.jdbi.v3.core.enums.DatabaseValue;

public enum FlagDataType {
    @DatabaseValue("1")
    String,
    @DatabaseValue("2")
    Boolean,
    @DatabaseValue("3")
    Float,
    @DatabaseValue("4")
    Double,
    @DatabaseValue("5")
    Integer,
    @DatabaseValue("6")
    Color;
}