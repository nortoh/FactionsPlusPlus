package dansplugins.factionsystem.models;

import java.lang.reflect.Type;

import dansplugins.factionsystem.constants.ConfigOptionType;

public class ConfigOption {
    private String name;
    private String description;
    private Object defaultValue;
    private ConfigOptionType type;
    private Boolean userSettable;

    public ConfigOption(String name, String description, ConfigOptionType type, Object defaultValue, Boolean userSettable) {
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

    public Boolean isUserSettable() {
        return this.userSettable;
    }
}