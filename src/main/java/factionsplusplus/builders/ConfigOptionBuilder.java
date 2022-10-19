package factionsplusplus.builders;

import java.lang.reflect.Type;

import factionsplusplus.constants.ConfigOptionType;
import factionsplusplus.models.ConfigOption;

public class ConfigOptionBuilder {

    public String name;
    public String description = null;
    public Object defaultValue = null;
    public ConfigOptionType type = ConfigOptionType.String;
    public Boolean userSettable = true;

    public ConfigOptionBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ConfigOptionBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ConfigOptionBuilder setDefaultValue(Object value) {
        this.defaultValue = value;
        return this;
    }

    public ConfigOptionBuilder setType(ConfigOptionType type) {
        this.type = type;
        return this;
    }

    public ConfigOptionBuilder notUserSettable() {
        this.userSettable = false;
        return this;
    }

    public ConfigOptionBuilder isBoolean() {
        this.type = ConfigOptionType.Boolean;
        return this;
    }

    public ConfigOptionBuilder isDouble() {
        this.type = ConfigOptionType.Double;
        return this;
    }

    public ConfigOptionBuilder isInteger() {
        this.type = ConfigOptionType.Integer;
        return this;
    }

    public ConfigOption create() {
        return new ConfigOption(name, description, type, defaultValue, userSettable);
    }
}