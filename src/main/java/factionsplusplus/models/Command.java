package factionsplusplus.models;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.commands.abs.ColorTranslator;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

public class Command implements ColorTranslator {
    public static final String LOCALE_PREFIX = "Locale_";
    private String name;
    private String[] aliases;
    private String[] requiredPermissions;
    private String description;
    private Boolean subCommand;
    private Boolean requiresPlayerExecution;
    private Boolean requiresFactionMembership;
    private Boolean requiresFactionOwnership;
    private Boolean requiresFactionOfficership;
    private Boolean requiresSubCommand;
    private Boolean requiresNoFactionMembership;
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

    public Boolean isSubCommand() {
        return this.subCommand;
    }

    public Boolean shouldRequirePlayerExecution() {
        return this.requiresPlayerExecution;
    }

    public Boolean shouldRequireFactionMembership() {
        return this.requiresFactionMembership;
    }

    public Boolean shouldRequireFactionOfficership() {
        return this.requiresFactionOfficership;
    }

    public Boolean shouldRequireFactionOwnership() {
        return this.requiresFactionOwnership;
    }

    public Boolean shouldRequireNoFactionMembership() {
        return this.requiresNoFactionMembership;
    }

    public Boolean shouldRequireSubCommand() {
        return this.requiresSubCommand;
    }

    public String getExecutorMethod() {
        return this.executorMethod;
    }

    public Boolean hasSubCommands() {
        return this.subcommands.size() > 0;
    }

    public HashMap<String, Command> getSubCommands() {
        return this.subcommands;
    }

    public Command getSubCommand(String name) {
        // TODO: search both name and aliases
        return this.subcommands.get(name);
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

    public Command setIsSubCommand(Boolean isSubCommand) {
        this.subCommand = isSubCommand;
        return this;
    }

    public Command setDescription(String description) {
        this.description = description;
        return this;
    }

    public Command setRequiresPlayerExecution(Boolean value) {
        this.requiresPlayerExecution = value;
        return this;
    }

    public Command setRequiresFactionMembership(Boolean value) {
        this.requiresFactionMembership = value;
        return this;
    }

    public Command setRequiresFactionOwnership(Boolean value) {
        this.requiresFactionOwnership = value;
        return this;
    }

    public Command setRequiresFactionOfficership(Boolean value) {
        this.requiresFactionOfficership = value;
        return this;
    }

    public Command setRequiresNoFactionMembership(Boolean value) {
        this.requiresNoFactionMembership = value;
        return this;
    }

    public Command setRequiresSubCommand(Boolean value) {
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

    public List<String> handleTabComplete(Player player, String[] args) {
        return new ArrayList<>();
    }

    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    public MessageBuilder constructMessage(String localizationKey) {
        return new MessageBuilder(localizationKey);
    }

    /**
     * Parent method to conduct tab completion. This will check permissions first, then hand to the child.
     */
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (this.requiresPlayerExecution) {
            if (!(sender instanceof Player)) {
                return this.handleTabComplete((Player)sender, args);
            }
            return null;
        }
        return this.handleTabComplete(sender, args);
    }
}