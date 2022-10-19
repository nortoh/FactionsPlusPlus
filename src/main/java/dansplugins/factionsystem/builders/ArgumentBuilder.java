package dansplugins.factionsystem.builders;

import dansplugins.factionsystem.constants.ArgumentType;

public class ArgumentBuilder {
    public String description = null;
    public Boolean required = true;
    public ArgumentType type = ArgumentType.Any;
    public Boolean shouldConsumeRestOfArguments = false;
    public Boolean requiresDoubleQuotes = false;
    public String[] permissionsIfNull = new String[]{};
    public String[] permissionsIfNotNull = new String[]{};
    public Object defaultValue = null;

    public ArgumentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }
    
    public ArgumentBuilder setType(ArgumentType type) {
        this.type = type;
        return this;
    }

    public ArgumentBuilder isRequired() {
        this.required = true;
        return this;
    }

    public ArgumentBuilder isOptional() {
        this.required = false;
        return this;
    }

    public ArgumentBuilder setDefaultValue(Object value) {
        this.defaultValue = value;
        return this;
    }

    public ArgumentBuilder requiresPermissionsIfNull(String... permissions) {
        this.permissionsIfNull = permissions;
        return this;
    }

    public ArgumentBuilder requiresPermissionsIfNotNull(String... permissions) {
        this.permissionsIfNotNull = permissions;
        return this;
    }

    public ArgumentBuilder expectsDoubleQuotes() {
        this.requiresDoubleQuotes = true;
        return this;
    }

    public ArgumentBuilder consumesAllLaterArguments() {
        this.shouldConsumeRestOfArguments = true;
        return this;
    }

    public ArgumentBuilder expectsString() {
        this.type = ArgumentType.String;
        return this;
    }

    public ArgumentBuilder expectsBoolean() {
        this.type = ArgumentType.Boolean;
        return this;
    }

    public ArgumentBuilder expectsFloat() {
        this.type = ArgumentType.Float;
        return this;
    }

    public ArgumentBuilder exceptsDouble() {
        this.type = ArgumentType.Double;
        return this;
    }

    public ArgumentBuilder expectsInteger() {
        this.type = ArgumentType.Integer;
        return this;
    }

    public ArgumentBuilder expectsAnyPlayer() {
        this.type = ArgumentType.Player;
        return this;
    }

    public ArgumentBuilder expectsOnlinePlayer() {
        this.type = ArgumentType.OnlinePlayer;
        return this;
    }

    public ArgumentBuilder expectsFaction() {
        this.type = ArgumentType.Faction;
        return this;
    }

    public ArgumentBuilder expectsFactionMember() {
        this.type = ArgumentType.FactionMember;
        return this;
    }

    public ArgumentBuilder expectsFactionOfficer() {
        this.type = ArgumentType.FactionOfficer;
        return this;
    }

    public ArgumentBuilder expectsAlliedFaction() {
        this.type = ArgumentType.AlliedFaction;
        return this;
    }

    public ArgumentBuilder expectsEnemyFaction() {
        this.type = ArgumentType.EnemyFaction;
        return this;
    }

    public ArgumentBuilder expectsVassaledFaction() {
        this.type = ArgumentType.VassaledFaction;
        return this;
    }

    public ArgumentBuilder expectsColor() {
        this.type = ArgumentType.Color;
        return this;
    }

    public ArgumentBuilder expectsFactionFlagName() {
        this.type = ArgumentType.FactionFlagName;
        return this;
    }

    public ArgumentBuilder expectsConfigOptionName() {
        this.type = ArgumentType.ConfigOptionName;
        return this;
    }
}