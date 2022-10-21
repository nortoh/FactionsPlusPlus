package factionsplusplus.models;

import java.util.Set;

import factionsplusplus.builders.ArgumentBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.constants.ArgumentType;

public class CommandArgument {
    private String description;
    private boolean required;
    private ArgumentType type;
    private boolean shouldConsumeRestOfArguments;
    private boolean requiresDoubleQuotes;
    private String[] permissionsIfNull = new String[]{};
    private String[] permissionsIfNotNull = new String[]{};
    private Object defaultValue;
    private String tabCompletionHandler;
    private Set<ArgumentFilterType> filters;

    public String getDescription() {
        return this.description;
    }

    public boolean isRequired() {
        return this.required;
    }

    public boolean expectsDoubleQuotes() {
        return this.requiresDoubleQuotes;
    }

    public String getTabCompletionHandler() {
        return this.tabCompletionHandler;
    }

    public Set<ArgumentFilterType> getFilters() {
        return this.filters;
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

    public boolean shouldConsumeAllArguments() {
        return this.shouldConsumeRestOfArguments;
    }

    public CommandArgument setDescription(String description) {
        this.description = description;
        return this;
    }

    public CommandArgument setRequired(boolean value) {
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

    public CommandArgument setShouldConsumeRestOfArguments(boolean value) {
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

    public CommandArgument setRequiresDoubleQuotes(boolean value) {
        this.requiresDoubleQuotes = value;
        return this;
    }

    public CommandArgument setTabCompletionHandler(String method) {
        this.tabCompletionHandler = method;
        return this;
    }

    public CommandArgument setFilters(Set<ArgumentFilterType> filters) {
        this.filters = filters;
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
            .setTabCompletionHandler(builder.tabCompletionHandler)
            .setFilters(builder.filters)
            .setShouldConsumeRestOfArguments(builder.shouldConsumeRestOfArguments);
    }
}