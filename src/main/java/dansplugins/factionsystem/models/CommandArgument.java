package dansplugins.factionsystem.models;

import dansplugins.factionsystem.builders.ArgumentBuilder;
import dansplugins.factionsystem.constants.ArgumentType;

public class CommandArgument {
    private String description;
    private Boolean required;
    private ArgumentType type;
    private Boolean shouldConsumeRestOfArguments;
    public String[] permissionsIfNull = new String[]{};
    public String[] permissionsIfNotNull = new String[]{};

    public String getDescription() {
        return this.description;
    }

    public Boolean isRequired() {
        return this.required;
    }

    public ArgumentType getType() {
        return this.type;
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

    public static CommandArgument fromBuilder(ArgumentBuilder builder) {
        return new CommandArgument()
            .setDescription(builder.description)
            .setRequired(builder.required)
            .setType(builder.type)
            .setNullPermissions(builder.permissionsIfNull)
            .setNotNullPermissions(builder.permissionsIfNotNull)
            .setShouldConsumeRestOfArguments(builder.shouldConsumeRestOfArguments);
    }
}