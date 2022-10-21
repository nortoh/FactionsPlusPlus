package factionsplusplus.models;

import factionsplusplus.constants.ConfigOptionType;

public class ConfigOption {
    private String name;
    private String description;
    private Object defaultValue;
    private ConfigOptionType type;
    private boolean userSettable;

    public ConfigOption(String name, String description, ConfigOptionType type, Object defaultValue, boolean userSettable) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.type = type;
        this.userSettable = userSettable;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public ConfigOptionType getType() {
        return this.type;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isUserSettable() {
        return this.userSettable;
    }
}