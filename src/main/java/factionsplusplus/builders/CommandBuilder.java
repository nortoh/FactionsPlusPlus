package factionsplusplus.builders;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class CommandBuilder {

    public String name;
    public String[] aliases = new String[]{};
    public String[] requiredPermissions = new String[]{};
    public String description = null;
    public String executorMethod = "execute";
    public boolean subCommand = false;
    public boolean requiresPlayerExecution = false;
    public boolean requiresFactionMembership = false;
    public boolean requiresFactionOwnership = false;
    public boolean requiresFactionOfficership = false;
    public boolean requiresSubCommand = false;
    public boolean requiresNoFactionMembership = false;
    public LinkedHashMap<String, ArgumentBuilder> arguments = new LinkedHashMap<>();
    public HashMap<String, CommandBuilder> subcommands = new HashMap<>();

    public CommandBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public CommandBuilder withAliases(String... aliases) {
        this.aliases = aliases;
        return this;
    }

    public CommandBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public CommandBuilder requiresPermissions(String... permissions) 
    {
        this.requiredPermissions = permissions;
        return this;
    }

    public CommandBuilder expectsFactionMembership() {
        this.requiresFactionMembership = true;
        return this;
    }

    public CommandBuilder expectsFactionOwnership() {
        this.requiresFactionOfficership = true;
        return this;
    }

    public CommandBuilder expectsFactionOfficership() {
        this.requiresFactionOfficership = true;
        return this;
    }

    public CommandBuilder expectsPlayerExecution() {
        this.requiresPlayerExecution = true;
        return this;
    }

    public CommandBuilder expectsNoFactionMembership() {
        this.requiresNoFactionMembership = true;
        return this;
    }

    public CommandBuilder isSubCommand() {
        this.subCommand = true;
        return this;
    }

    public CommandBuilder requiresSubCommand() {
        this.requiresSubCommand = true;
        return this;
    }

    public ArgumentBuilder createArgumentBuilder() {
        return new ArgumentBuilder();
    }

    public CommandBuilder createSubCommand() {
        return new CommandBuilder().isSubCommand();
    }

    public CommandBuilder addSubCommand(CommandBuilder builder) {
        builder.isSubCommand();
        this.subcommands.put(builder.name, builder);
        return this;
    }

    public CommandBuilder addArgument(String name, ArgumentBuilder builder) {
        this.arguments.put(name, builder);
        return this;
    }

    public CommandBuilder setExecutorMethod(String name) {
        this.executorMethod = name;
        return this;
    }
}