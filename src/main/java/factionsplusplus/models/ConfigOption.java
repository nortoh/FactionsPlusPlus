package factionsplusplus.models;

import factionsplusplus.constants.ConfigOptionType;

public class ConfigOption {
    private String name;
    private String description;
    private Object defaultValue;
    private ConfigOptionType type;
    private boolean userSettable;
    private boolean hidden;

    public ConfigOption(
        String name,
        String description,
        ConfigOptionType type,
        Object defaultValue,
        boolean userSettable,
        boolean hidden
    ) {
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

    public boolean isHidden() {
        return this.hidden;
    }
}