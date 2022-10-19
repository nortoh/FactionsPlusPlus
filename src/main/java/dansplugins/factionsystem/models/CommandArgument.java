package dansplugins.factionsystem.models;

import dansplugins.factionsystem.builders.ArgumentBuilder;
import dansplugins.factionsystem.constants.ArgumentType;

public class CommandArgument {
    private String description;
    private Boolean required;
    private ArgumentType type;
    private Boolean shouldConsumeRestOfArguments;
    private Boolean requiresDoubleQuotes;
    private String[] permissionsIfNull = new String[]{};
    private String[] permissionsIfNotNull = new String[]{};
    private Object defaultValue;

    public String getDescription() {
        return this.description;
    }

    public Boolean isRequired() {
        return this.required;
    }

    public Boolean expectsDoubleQuotes() {
        return this.requiresDoubleQuotes;
    }

    public ArgumentType getType() {
        return this.type;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public String[] getNullPermissions() {
        return this.permissionsIfNull;
    }

    public String[] getNotNullPermissions() {
        return this.permissionsIfNotNull;
    }

    public Boolean shouldConsumeAllArguments() {
        return this.shouldConsumeRestOfArguments;
    }

    public CommandArgument setDescription(String description) {
        this.description = description;
        return this;
    }

    public CommandArgument setRequired(Boolean value) {
        this.required = value;
        return this;
    }

    public CommandArgument setType(ArgumentType type) {
        this.type = type;
        return this;
    }

    public CommandArgument setDefaultValue(Object value) {
        this.defaultValue = value;
        return this;
    }

    public CommandArgument setShouldConsumeRestOfArguments(Boolean value) {
        this.shouldConsumeRestOfArguments = value;
        return this;
    }

    public CommandArgument setNullPermissions(String[] permissions) {
        this.permissionsIfNull = permissions;
        return this;
    }

    public CommandArgument setNotNullPermissions(String[] permissions) {
        this.permissionsIfNotNull = permissions;
        return this;
    }

    public CommandArgument setRequiresDoubleQuotes(Boolean value) {
        this.requiresDoubleQuotes = value;
        return this;
    }

    public static CommandArgument fromBuilder(ArgumentBuilder builder) {
        return new CommandArgument()
            .setDescription(builder.description)
            .setRequired(builder.required)
            .setType(builder.type)
            .setDefaultValue(builder.defaultValue)
            .setNullPermissions(builder.permissionsIfNull)
            .setNotNullPermissions(builder.permissionsIfNotNull)
            .setRequiresDoubleQuotes(builder.requiresDoubleQuotes)
            .setShouldConsumeRestOfArguments(builder.shouldConsumeRestOfArguments);
    }
}