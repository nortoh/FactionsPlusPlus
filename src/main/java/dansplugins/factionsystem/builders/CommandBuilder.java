package dansplugins.factionsystem.builders;

import java.util.HashMap;

public class CommandBuilder {

    public String name;
    public String[] aliases = new String[]{};
    public String[] requiredPermissions = new String[]{};
    public String description = null;
    public String executorMethod = "execute";
    public Boolean subCommand = false;
    public Boolean requiresPlayerExecution = false;
    public Boolean requiresFactionMembership = false;
    public Boolean requiresFactionOwnership = false;
    public Boolean requiresFactionOfficership = false;
    public Boolean requiresSubCommand = false;
    public HashMap<String, ArgumentBuilder> arguments = new HashMap<>();
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