package factionsplusplus.models;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.commands.abs.ColorTranslator;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

public class Command implements ColorTranslator {
    public static final String LOCALE_PREFIX = "Alias.";
    private String name;
    private String[] aliases;
    private String[] requiredPermissions;
    private String description;
    private boolean subCommand;
    private boolean requiresPlayerExecution;
    private boolean requiresFactionMembership;
    private boolean requiresFactionOwnership;
    private boolean requiresFactionOfficership;
    private boolean requiresSubCommand;
    private boolean requiresNoFactionMembership;
    private String executorMethod;
    private LinkedHashMap<String, CommandArgument> arguments = new LinkedHashMap<>();
    private HashMap<String, Command> subcommands = new HashMap<>();

    public Command(CommandBuilder builder) {
        this.setName(builder.name);
        this.setAliases(builder.aliases);
        this.setRequiredPermissions(builder.requiredPermissions);
        this.setIsSubCommand(builder.subCommand);
        this.setDescription(builder.description);
        this.setRequiresPlayerExecution(builder.requiresPlayerExecution);
        this.setRequiresFactionMembership(builder.requiresFactionMembership);
        this.setRequiresFactionOwnership(builder.requiresFactionOwnership);
        this.setRequiresFactionOfficership(builder.requiresFactionOfficership);
        this.setRequiresNoFactionMembership(builder.requiresNoFactionMembership);
        this.setRequiresSubCommand(builder.requiresSubCommand);
        this.setExecutorMethod(builder.executorMethod);
        for (Map.Entry<String, CommandBuilder> entry : builder.subcommands.entrySet()) {
            this.addSubCommand(entry.getKey(), new Command(entry.getValue()));
        }
        for (Map.Entry<String, ArgumentBuilder> entry : builder.arguments.entrySet()) {
            this.addArgument(entry.getKey(), CommandArgument.fromBuilder(entry.getValue()));
        }
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String[] getAliases() {
        return this.aliases;
    }

    public String[] getRequiredPermissions() {
        return this.requiredPermissions;
    }

    public boolean isSubCommand() {
        return this.subCommand;
    }

    public boolean shouldRequirePlayerExecution() {
        return this.requiresPlayerExecution;
    }

    public boolean shouldRequireFactionMembership() {
        return this.requiresFactionMembership;
    }

    public boolean shouldRequireFactionOfficership() {
        return this.requiresFactionOfficership;
    }

    public boolean shouldRequireFactionOwnership() {
        return this.requiresFactionOwnership;
    }

    public boolean shouldRequireNoFactionMembership() {
        return this.requiresNoFactionMembership;
    }

    public boolean shouldRequireSubCommand() {
        return this.requiresSubCommand;
    }

    public String getExecutorMethod() {
        return this.executorMethod;
    }

    public boolean hasSubCommands() {
        return this.subcommands.size() > 0;
    }

    public HashMap<String, Command> getSubCommands() {
        return this.subcommands;
    }

    public Command getSubCommand(String nameSearch, boolean onlySearchRootNames) {
        // Look for exact first
        if (this.subcommands.containsKey(nameSearch.toLowerCase())) return this.subcommands.get(nameSearch.toLowerCase());
        if (onlySearchRootNames) return null; // we're not going to do any alias searching here
        Optional<Command> command = this.subcommands.values().stream()
            .filter(c -> Arrays.asList(c.getAliases()).contains(nameSearch))
            .findFirst();
        return command.orElse(null);
    }

    public Command getSubCommand(String nameSearch) {
        return this.getSubCommand(nameSearch, false);
    }
    
    public LinkedHashMap<String, CommandArgument> getArguments() {
        return this.arguments;
    }

    public int getRequiredArgumentCount() {
        int requiredArgumentCount = this.requiresSubCommand ? 1 : 0;
        for (Map.Entry<String, CommandArgument> entry : this.arguments.entrySet()) {
            if (entry.getValue().isRequired()) requiredArgumentCount++;
        }
        return requiredArgumentCount;
    }

    public String buildSyntax() {
        StringJoiner output = new StringJoiner(" ");
        if (this.hasSubCommands()) {
            StringJoiner joiner = new StringJoiner("/");
            for (Map.Entry<String, Command> entry : this.subcommands.entrySet()) {
                joiner.add(entry.getKey());
            }
            if (!this.requiresSubCommand) {
                output.add(String.format("[%s]", joiner.toString()));
            } else {
                output.add(joiner.toString());
            }
        }
        for (Map.Entry<String, CommandArgument> entry : this.arguments.entrySet()) {
            String encapsulationCharacter = "";
            if (entry.getValue().expectsDoubleQuotes()) encapsulationCharacter = "\"";
            if (entry.getValue().isRequired()) {
                output.add(String.format("<%s%s%s>", encapsulationCharacter, entry.getKey(), encapsulationCharacter));
            } else {
                output.add(String.format("[<%s%s%s>]", encapsulationCharacter, entry.getKey(), encapsulationCharacter));
            }
        }
        return output.toString();
    }

    public Command setName(String name) {
        this.name = name;
        return this;
    }

    public Command setAliases(String[] aliases) {
        this.aliases = aliases;
        return this;
    }

    public Command setRequiredPermissions(String[] permissions) {
        this.requiredPermissions = permissions;
        return this;
    }

    public Command setIsSubCommand(boolean isSubCommand) {
        this.subCommand = isSubCommand;
        return this;
    }

    public Command setDescription(String description) {
        this.description = description;
        return this;
    }

    public Command setRequiresPlayerExecution(boolean value) {
        this.requiresPlayerExecution = value;
        return this;
    }

    public Command setRequiresFactionMembership(boolean value) {
        this.requiresFactionMembership = value;
        return this;
    }

    public Command setRequiresFactionOwnership(boolean value) {
        this.requiresFactionOwnership = value;
        return this;
    }

    public Command setRequiresFactionOfficership(boolean value) {
        this.requiresFactionOfficership = value;
        return this;
    }

    public Command setRequiresNoFactionMembership(boolean value) {
        this.requiresNoFactionMembership = value;
        return this;
    }

    public Command setRequiresSubCommand(boolean value) {
        this.requiresSubCommand = value;
        return this;
    }

    public Command addSubCommand(String name, Command command) {
        this.subcommands.put(name, command);
        return this;
    }

    public Command addArgument(String name, CommandArgument argument) {
        this.arguments.put(name, argument);
        return this;
    }

    public Command setExecutorMethod(String name) {
        this.executorMethod = name;
        return this;
    }
    
    public String toString() {
        return this.description;
    }

    public MessageBuilder constructMessage(String localizationKey) {
        return new MessageBuilder(localizationKey);
    }
}