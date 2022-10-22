/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.commands.*;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionFlag;
import factionsplusplus.utils.Logger;
import factionsplusplus.utils.RelationChecker;
import factionsplusplus.utils.PlayerUtils;
import factionsplusplus.utils.StringUtils;
import factionsplusplus.utils.extended.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandArgument;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.ConfigOption;
import factionsplusplus.repositories.CommandRepository;

import factionsplusplus.builders.MessageBuilder;

import java.util.Collection;
import java.util.Arrays;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class CommandService implements TabCompleter {
    private final LocaleService localeService;
    private final FactionsPlusPlus factionsPlusPlus;
    private final ConfigService configService;
    private final PlayerService playerService;
    private final MessageService messageService;
    private final CommandRepository commandRepository;
    private final DataService dataService;
    private final FactionService factionService;

    @Inject
    public CommandService(
        LocaleService localeService,
        FactionsPlusPlus factionsPlusPlus,
        ConfigService configService,
        PlayerService playerService,
        MessageService messageService,
        CommandRepository commandRepository,
        FactionService factionService,
        DataService dataService
    ) {
        this.localeService = localeService;
        this.factionsPlusPlus = factionsPlusPlus;
        this.configService = configService;
        this.playerService = playerService;
        this.messageService = messageService;
        this.commandRepository = commandRepository;
        this.dataService = dataService;
        this.factionService = factionService;
    }

    public void registerCommands() {
        Class[] coreCommands = new Class[]{
            AllyCommand.class,
            AutoClaimCommand.class,
            BreakAllianceCommand.class,
            BypassCommand.class,
            ChatCommand.class,
            CheckAccessCommand.class,
            CheckClaimCommand.class,
            ClaimCommand.class,
            ConfigCommand.class,
            CreateCommand.class,
            DeclareIndependenceCommand.class,
            DeclareWarCommand.class,
            DemoteCommand.class,
            DescCommand.class,
            DisbandCommand.class,
            DuelCommand.class,
            FlagsCommand.class,
            ForceCommand.class,
            GateCommand.class,
            GrantAccessCommand.class,
            GrantIndependenceCommand.class,
            HelpCommand.class,
            HomeCommand.class,
            InfoCommand.class,
            InviteCommand.class,
            InvokeCommand.class,
            JoinCommand.class,
            KickCommand.class,
            LawCommand.class,
            LawsCommand.class,
            LeaveCommand.class,
            ListCommand.class,
            LockCommand.class,
            MakePeaceCommand.class,
            MapCommand.class,
            MembersCommand.class,
            PowerCommand.class,
            PrefixCommand.class,
            PromoteCommand.class,
            RenameCommand.class,
            ResetPowerLevelsCommand.class,
            RevokeAccessCommand.class,
            SetHomeCommand.class,
            StatsCommand.class,
            SwearFealtyCommand.class,
            TransferCommand.class,
            UnclaimallCommand.class,
            UnclaimCommand.class,
            UnlockCommand.class,
            VassalizeCommand.class,
            VersionCommand.class,
            WhoCommand.class
        };
        for (Class commandClass : coreCommands) {
            this.registerCommand(commandClass);
        }
    }

    public Command registerCommand(Class commandClass) {
        Command command = (Command)this.factionsPlusPlus.getInjector().getInstance(commandClass);
        this.commandRepository.add(command);
        return command;
    }

    // TODO: reimplement
    public void loadCommandNames(Command command) {

    }

    public List<String> checkPermissions(CommandSender sender, String[] permissions) {
        List<String> missingPermissions = new ArrayList<>();
        if (permissions.length == 0) return missingPermissions;
        boolean hasPermission = false;
        for (String perm : permissions) {
            hasPermission = sender.hasPermission(perm);
            if (hasPermission) return missingPermissions;
            missingPermissions.add(perm);
        }
        return missingPermissions;
    }

    public boolean processCommand(Command parentCommand, Command command, CommandSender sender, ArrayList<String> arguments, CommandContext context) {
        // If context is null, create a new command context. This usually means we're a root command.
        if (context == null) {
            context = this.factionsPlusPlus.getInjector().getInstance(CommandContext.class);
            context.setSender(sender);
            context.setRawArguments(Arrays.copyOf(arguments.toArray(), arguments.size(), String[].class));
        }

        context.addCommandName(command.getName());

        // If arguments are missing, let the user know.
        if (command.getRequiredArgumentCount() > arguments.size()) {
            this.messageService.sendInvalidSyntaxMessage(sender, context.getCommandNames(), command.buildSyntax());
            return false;
        }

        // Check permissions. If permissions are missing, let the user know.
        List<String> missingPermissions = this.checkPermissions(sender, command.getRequiredPermissions());
        if (missingPermissions.size() > 0) {
            this.messageService.sendPermissionMissingMessage(sender, missingPermissions);
            return false;
        }

        // Check if we require a player context (i.e. not the console)
        if (command.shouldRequirePlayerExecution()) {
            if (!(sender instanceof Player)) {
                context.replyWith("OnlyPlayersCanUseCommand");
                return false;
            }
        }

        // Check if this command should require faction specific stuff
        Faction playerFaction = null;
        if (! context.isConsole()) {
            playerFaction = this.dataService.getPlayersFaction((OfflinePlayer)sender);
            context.setExecutorsFaction(playerFaction);
        }
        if (command.shouldRequireFactionMembership() || command.shouldRequireFactionOfficership() || command.shouldRequireFactionOwnership()) {
            if (playerFaction == null) {
                context.replyWith("AlertMustBeInFactionToUseCommand");
                return false;
            }
        }

        if (command.shouldRequireNoFactionMembership()) {
            if (playerFaction != null) {
                context.replyWith("AlertAlreadyInFaction");
                return false;
            }
        }

        // Faction owner?
        if (command.shouldRequireFactionOwnership()) {
            if (context.isConsole()) return false; // bail if console
            if (! playerFaction.getOwner().equals(((Player)sender).getUniqueId())) {
                context.replyWith("AlertMustBeOwnerToUseCommand");
                return false;
            }
        }

        // Faction officer or owner
        if (command.shouldRequireFactionOfficership()) {
            if (context.isConsole()) return false; // bail if console
            UUID senderUUID = ((Player)sender).getUniqueId();
            if (! playerFaction.getOwner().equals(senderUUID) && ! playerFaction.isOfficer(senderUUID)) {
                context.replyWith("AlertMustBeOwnerOrOfficeToUseCommand");
                return false;
            }
        }

        // Check if we have a subcommand
        if (command.hasSubCommands()) {
            Command newCommand = arguments.size() > 0 ? command.getSubCommand(arguments.get(0)) : null;
            if (newCommand == null) {
                // If a subcommand is required, we bail now.
                if (command.shouldRequireSubCommand()) {
                    this.messageService.sendInvalidSyntaxMessage(sender, context.getCommandNames(), command.buildSyntax());
                    return false;
                }
                // Otherwise, we execute the normal executor with the remaining parameters.
            } else {
                // Remove the subcommand from arguments
                arguments.remove(0);
                // Go!
                return this.processCommand(parentCommand, newCommand, sender, arguments, context);
            }
        }

        // Process arguments
        for (Map.Entry<String, CommandArgument> entry : command.getArguments().entrySet()) {
            String argumentName = entry.getKey();
            CommandArgument argument = entry.getValue();
            // If we're on the last argument, we have special things to do (namely, permission checks for that argument)
            String[] permissionsToCheck;
            if (
                (arguments.size() < 2)
                || argument.shouldConsumeAllArguments()
            ) {
                switch(arguments.size()) {
                    case 0:
                        permissionsToCheck = argument.getNullPermissions();
                        break;
                    default:
                        permissionsToCheck = argument.getNotNullPermissions();
                        break;
                }
                missingPermissions = this.checkPermissions(sender, permissionsToCheck);
                if (missingPermissions.size() > 0) {
                    this.messageService.sendPermissionMissingMessage(sender, missingPermissions);
                    return false;
                }
            }

            // If argument should consume the remainder of arguments, so be it. We can't handle any arguments after this.
            if (argument.shouldConsumeAllArguments() && arguments.size() > 0) {
                String newArguments = String.join(" ", (ArrayList)arguments.clone());
                arguments.clear();
                arguments.add(newArguments);
            }

            // Pop the next argument (what we should be using)
            if (arguments.size() > 0) {
                String argumentData = arguments.remove(0);
                // Check if the argument should start and end with double quotes, if so, append all other arguments until we get another double quote
                if (argumentData.startsWith("\"") && argument.expectsDoubleQuotes()) {
                    boolean foundEnd = false;
                    // Remove the opening quote
                    argumentData = argumentData.substring(1);
                    // Handle one word with double quotes
                    if (argumentData.endsWith("\"")) foundEnd = true;
                    while (!arguments.isEmpty() && !foundEnd) {
                        if (arguments.get(0).endsWith("\"")) foundEnd = true;
                        argumentData = argumentData + " " + arguments.remove(0);
                    }
                    if (!foundEnd) {
                        this.messageService.sendInvalidSyntaxMessage(sender, context.getCommandNames(), command.buildSyntax());
                        return false;
                    }
                    argumentData = argumentData.substring(0, argumentData.length() - 1); // remove closing quote
                }
                Object parsedArgumentData = null;
                Faction faction = null;
                OfflinePlayer player = null;
                boolean sendInvalidMessage = false;
                switch(argument.getType()) {
                    case Faction:
                        faction = this.getAnyFaction(context, argumentData);
                        if (faction != null) break;
                        return false;
                    case ConfigOptionName:
                        ConfigOption option = this.configService.getConfigOption(argumentData);
                        if (option != null) {
                            parsedArgumentData = option;
                            break;
                        }
                        context.replyWith(
                            new MessageBuilder("ConfigOptionDoesNotExist")
                                .with("option", argumentData)
                        );
                        return false;
                    case Player:
                        player = this.getPlayer(context, argumentData);
                        if (player != null) break;
                        return false;
                    case FactionMember:
                        player = this.getFactionMember(context, argumentData);
                        if (player != null) break;
                        return false;
                    case FactionOfficer:
                        player = this.getFactionOfficer(context, argumentData);
                        if (player != null) break;
                        return false;
                    case AlliedFaction:
                        faction = this.getAllyFaction(context, argumentData);
                        if (faction != null) break;
                        return false;
                    case EnemyFaction:
                        faction = this.getEnemyFaction(context, argumentData);
                        if (faction != null) break;
                        return false;
                    case VassaledFaction:
                        faction = this.getVassaledFaction(context, argumentData);
                        if (faction != null) break;
                        return false;
                    case OnlinePlayer:
                        Player onlinePlayer = Bukkit.getPlayer(argumentData);
                        if (onlinePlayer != null) {
                            parsedArgumentData = onlinePlayer;
                            break;
                        }            
                        context.replyWith(
                            new MessageBuilder("PlayerNotFound")
                                .with("name", argumentData)
                        );
                        return false;
                    case FactionFlagName:
                        FactionFlag flag = this.getFactionFlag(context, argumentData);
                        if (flag != null) {
                            parsedArgumentData = flag;
                            break;
                        }
                        return false;
                    case Integer:
                        Integer intValue = StringUtils.parseAsInteger(argumentData);
                        if (intValue != null) {
                            parsedArgumentData = intValue;
                            break;
                        }
                        sendInvalidMessage = true;
                        break;
                    case Double:
                        Double doubleValue = StringUtils.parseAsDouble(argumentData);
                        if (doubleValue != null) {
                            parsedArgumentData = doubleValue;
                            break;
                        }
                        sendInvalidMessage = true;
                        break;
                    case Boolean:
                        Boolean boolValue = StringUtils.parseAsBoolean(argumentData);
                        if (boolValue != null) {
                            parsedArgumentData = boolValue;
                            break;
                        }
                        sendInvalidMessage = true;
                        break;
                    default:
                        parsedArgumentData = argumentData;
                        break;
                }
                if (sendInvalidMessage) {
                    this.messageService.sendInvalidSyntaxMessage(sender, context.getCommandNames(), command.buildSyntax());
                    return false;
                }
                if (player != null) parsedArgumentData = player;
                if (faction != null) parsedArgumentData = faction;
                context.addArgument(argumentName, parsedArgumentData);
            } else {
                context.addArgument(argumentName, argument.getDefaultValue());
                continue;
            }
        }


        // Execute!
        try {
            Method executor = parentCommand.getClass().getDeclaredMethod(command.getExecutorMethod(), CommandContext.class);
            executor.invoke(parentCommand, context);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    private FactionFlag getFactionFlag(CommandContext context, String argumentData) {
        final FactionFlag flag = context.getExecutorsFaction().getFlag(argumentData);
        if (flag == null) {
            context.replyWith(
                new MessageBuilder("InvalidFactionFlag")
                    .with("flag", argumentData)
            );
        }
        return flag;
    }

    private Faction getAnyFaction(CommandContext context, String argumentData) {
        final Faction faction = this.dataService.getFaction(argumentData);
        if (faction == null) {
            context.replyWith(
                new MessageBuilder("FactionNotFound")
                    .with("faction", argumentData)
            );
        }
        return faction;
    }

    private Faction getAllyFaction(CommandContext context, String argumentData) {
        final Faction faction = this.getAnyFaction(context, argumentData);
        if (faction != null) {
            if (context.getExecutorsFaction().isAlly(faction.getID())) {
                return faction;
            }
            context.replyWith(
                new MessageBuilder("AlertNotAllied")
                    .with("faction", faction.getName())
            );
        }
        return null;
    }

    private Faction getEnemyFaction(CommandContext context, String argumentData) {
        final Faction faction = this.getAnyFaction(context, argumentData);
        if (faction != null) {
            if (context.getExecutorsFaction().isEnemy(faction.getID())) {
                return faction;
            }
            context.replyWith("FactionNotEnemy");
        }
        return null;
    }

    private Faction getVassaledFaction(CommandContext context, String argumentData) {
        final Faction faction = this.getAnyFaction(context, argumentData);
        if (faction != null) {
            if (faction.isLiege(context.getExecutorsFaction().getID())) {
                return faction;
            }
            context.replyWith("FactionIsNotVassal");
        }
        return null;
    }

    private OfflinePlayer getPlayer(CommandContext context, String argumentData) {
        final OfflinePlayer player = PlayerUtils.parseAsPlayer(argumentData);
        if (player == null) {
            context.replyWith(
                new MessageBuilder("PlayerNotFound")
                    .with("name", argumentData)
            );
        }
        return player;
    }

    private OfflinePlayer getFactionMember(CommandContext context, String argumentData) {
        final OfflinePlayer player = this.getPlayer(context, argumentData);
        if (player != null) {
            if (context.getExecutorsFaction().isMember(player.getUniqueId())) {
                return player;
            }
            context.replyWith("PlayerIsNotMemberOfFaction");
        }
        return null;
    }

    private OfflinePlayer getFactionOfficer(CommandContext context, String argumentData) {
        final OfflinePlayer player = this.getFactionMember(context, argumentData);
        if (player != null) {
            if (context.getExecutorsFaction().isOfficer(player.getUniqueId())) {
                return player;
            }
            context.replyWith("PlayerIsNotOfficerOfFaction");
        }
        return null;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        // no arguments check
        if (args.length == 0) {
            this.localeService.getStrings("PluginInfo")
                .forEach(s -> {
                    s = s.replace("#version", this.factionsPlusPlus.getVersion()).replace("#dev", this.factionsPlusPlus.getDescription().getAuthors().toString());
                    this.messageService.send(sender, s);
                });
            return true;
        }

        // Convert arguments to an array list
        ArrayList<String> argumentList = new ArrayList<>();
        argumentList.addAll(Arrays.asList(args));

        // Get & remove command name
        String commandName = argumentList.remove(0);

        // Try to find the command
        Command command = this.commandRepository.get(commandName);

        // Let the user know it wasn't found, if it wasn't found
        if (command == null) {
            this.messageService.sendLocalizedMessage(sender, "CommandNotRecognized");
            return false;
        }

        return this.processCommand(command, command, sender, argumentList, null);
    }

    private ArrayList<String> getSubCommandNamesForSender(CommandSender sender) {
        ArrayList<String> commandNames = new ArrayList<String>();
        for (Command subCommand : this.commandRepository.all().values()) {
            if (this.senderCanAccessCommand(sender, subCommand)) commandNames.add(subCommand.getName().toLowerCase());
        }
        return commandNames;
    }

    private boolean senderCanAccessCommand(CommandSender sender, Command command) {
        // Can console use this command?
        if (!(sender instanceof Player)) {
            if (command.shouldRequirePlayerExecution()) return false;
            return true; // no need to check permissions for console
        }
        if (this.checkPermissions(sender, command.getRequiredPermissions()).size() == 0) return true;
        return false;
    }

    private ArrayList<String> findCommandsStartingWith(String string, ArrayList<String> commandNames) {
        ArrayList<String> result = new ArrayList<String>();
        for (String commandName : commandNames) {
            if (commandName.startsWith(string.toLowerCase())) result.add(commandName);
        }
        return result;
    }

    public List<String> handleTabCompletionForCommand(CommandSender sender, List<Command> commandStack, ArrayList<String> argumentList, int argumentIndex) {
        ArrayList<String> results = new ArrayList<>();
        if (argumentList.isEmpty()) return results; // nothing left to do
        Command currentCommand = null;
        if (commandStack.isEmpty()) {
            // We're at the root
            String rootCommandName = argumentList.remove(0);
            if (argumentList.size() == 0) return this.findCommandsStartingWith(rootCommandName, this.getSubCommandNamesForSender(sender));
            currentCommand = this.commandRepository.get(rootCommandName);
            if (currentCommand == null) return results;
            commandStack.add(currentCommand);
        } else currentCommand = commandStack.get(commandStack.size() - 1);
        // Bail if nothing left to do
        if (!currentCommand.hasSubCommands() && currentCommand.getArguments().isEmpty()) return results;
        // Look for subcommands first
        if (currentCommand.hasSubCommands()) {
            String nextArg = argumentList.get(0); // we don't remove here because it could be a consumes all argument
            // Look for exact matches first
            Command subCommand = currentCommand.getSubCommand(nextArg);
            if (subCommand != null) {
                argumentList.remove(0); // remove subcommand
                commandStack.add(subCommand);
                return this.handleTabCompletionForCommand(sender, commandStack, argumentList, argumentIndex);
            }
            if (currentCommand.shouldRequireSubCommand() && argumentList.size() > 1) return results;
            for (Command commandObj : currentCommand.getSubCommands().values()) {
                if (
                    this.senderCanAccessCommand(sender, commandObj) &&
                    commandObj.getName().toLowerCase().startsWith(nextArg.toLowerCase())
                ) { 
                    results.add(commandObj.getName().toLowerCase());
                }
            }
        }

        // now process arguments
        // TODO: come back to this logic, I think it may be running too much when extra parameters are given past the expected amount
        if (currentCommand.getArguments().size() >= argumentIndex+1) {
            String argumentName = (String)currentCommand.getArguments().keySet().toArray()[argumentIndex];
            final CommandArgument argument = currentCommand.getArguments().get(argumentName);
            if (argument.shouldConsumeAllArguments()) {
                String newArguments = String.join(" ", (ArrayList)argumentList.clone());
                argumentList.clear();
                argumentList.add(newArguments);
            }
            String argumentData = argumentList.remove(0);
            boolean moveToNextArgumentIfPresent = true;
            if (argumentData.startsWith("\"") && argument.expectsDoubleQuotes()) {
                boolean foundEnd = false;
                // Remove the opening quote
                argumentData = argumentData.substring(1);
                // Handle one word with double quotes
                if (argumentData.endsWith("\"")) foundEnd = true;
                while (!argumentList.isEmpty() && !foundEnd) {
                    if (argumentList.get(0).endsWith("\"")) foundEnd = true;
                    argumentData = argumentData + " " + argumentList.remove(0);
                }
                if (!foundEnd) moveToNextArgumentIfPresent = false;
                else argumentData = argumentData.substring(0, argumentData.length() - 1); // remove closing quote
            }

            // Go to next argument, no need to generate results for arguments already present. The command will validate them.
            if (moveToNextArgumentIfPresent && ! argumentList.isEmpty()) {
                // Bail if no more arguments for command
                argumentIndex++;
                if (argumentIndex >= currentCommand.getArguments().size()) return results;
                return this.handleTabCompletionForCommand(sender, commandStack, argumentList, argumentIndex);
            }

            // Handle (sub)commands wanting to handle their own tab completion
            if (argument.getTabCompletionHandler() != null) {
                try {
                    Method executor = argument.getClass().getDeclaredMethod(argument.getTabCompletionHandler(), CommandSender.class, ArrayList.class);
                    return (List<String>)executor.invoke(sender, argumentList);
                } catch(Exception e) {
                    return results;
                }
            }

            // Lambdas need "effectively final" variables so we need a copy of argumentData that is final
            final String argumentText = argumentData.toLowerCase();
            // Try to get players faction (not always going to be a player)
            Faction playersFaction = null;
            Player player = null;
            if (sender instanceof Player) {
                player = (Player)sender;
                playersFaction = this.dataService.getFactionRepository().getForPlayer(player);
            }

            // Console types
            switch(argument.getType()) {
                case ConfigOptionName:
                    results.addAll(this.configService.getConfigOptions().keySet());
                    return results;
                case FactionFlagName:
                    results.addAll(this.factionService.getDefaultFlags().keySet());
                    return results;
                case Faction:
                    return this.applyFactionFilters(
                            this.dataService.getFactionRepository().all().values().stream()
                                .filter(faction -> faction.getName().toLowerCase().startsWith(argumentText)),
                            playersFaction,
                            argument
                        )
                        .map(Faction::getName)
                        .map(name -> this.quoteifyIfNeeded(name, argument))
                        .collect(Collectors.toList());
                case OnlinePlayer:
                    return this.applyPlayerFilters(
                            Bukkit.getOnlinePlayers().stream()
                                .filter(p -> p.getName().toLowerCase().startsWith(argumentText)),
                            player,
                            playersFaction,
                            argument
                        )
                        .map(OfflinePlayer::getName)
                        .map(name -> this.quoteifyIfNeeded(name, argument))
                        .collect(Collectors.toList());
                case Player:
                    return this.applyPlayerFilters(
                            this.dataService.getPlayerRecordRepository().all().stream()
                                .map(record -> Bukkit.getOfflinePlayer(record.getPlayerUUID()))
                                .filter(p -> p.getName().toLowerCase().startsWith(argumentText)),
                            player,
                            playersFaction,
                            argument
                        )
                        .map(OfflinePlayer::getName)
                        .collect(Collectors.toList());
                case Boolean:
                    results.add("true");
                    results.add("false");
                case String:
                case Integer:
                case Double:
                case Any:
                    return results;
                default:
                    break;
            }

            // Player types
            if (!(sender instanceof Player)) return results;
            if (playersFaction == null) return results;

            switch(argument.getType()) {
                case AlliedFaction:
                    return playersFaction.getAllies().stream()
                        .map(id -> this.dataService.getFaction(id).getName().toLowerCase())
                        .filter(name -> name.startsWith(argumentText))
                        .collect(Collectors.toList());
                case EnemyFaction:
                    return playersFaction.getAllies().stream()
                        .map(id -> this.dataService.getFaction(id).getName().toLowerCase())
                        .filter(name -> name.startsWith(argumentText))
                        .collect(Collectors.toList());
                case VassaledFaction:
                    return playersFaction.getVassals().stream()
                        .map(id -> this.dataService.getFaction(id).getName().toLowerCase())
                        .filter(name -> name.startsWith(argumentText))
                        .collect(Collectors.toList());
                case FactionMember:
                    return playersFaction.getMembers().stream()
                        .map(id -> Bukkit.getOfflinePlayer(id).getName().toLowerCase())
                        .filter(name -> name.startsWith(argumentText))
                        .collect(Collectors.toList());
                case FactionOfficer:
                    return playersFaction.getOfficerList().stream()
                        .map(id -> Bukkit.getOfflinePlayer(id).getName().toLowerCase())
                        .filter(name -> name.startsWith(argumentText))
                        .collect(Collectors.toList());
                default:
                    break;
            }
        }
        return results;
    }

    private Stream<Faction> applyFactionFilters(Stream<Faction> input, Faction faction, CommandArgument argument) {
        if (faction == null) return input;
        for (ArgumentFilterType type : argument.getFilters()) {
            switch(type) {
                case Allied:
                    input = input.filter(f -> faction.isAlly(f.getID()));
                    break;
                case NotAllied:
                    input = input.filter(f -> !faction.isAlly(f.getID()));
                    break;
                case NotOwnFaction:
                    input = input.filter(f -> !faction.equals(f));
                    break;
                case Enemy:
                    input = input.filter(f -> faction.isEnemy(f.getID()));
                    break;
                case NotEnemy:
                    input = input.filter(f -> !faction.isEnemy(f.getID()));
                    break;
                case Vassal:
                    input = input.filter(f -> faction.isVassal(f.getID()));
                    break;
                case NotVassal:
                    input = input.filter(f -> !faction.isVassal(f.getID()));
                    break;
                case OfferedVassalization:
                    input = input.filter(f -> f.hasBeenOfferedVassalization(faction.getID()));
                    break;
                default:
                    break;
            }
        }
        return input;
    }

    private Stream<? extends OfflinePlayer> applyPlayerFilters(Stream<? extends OfflinePlayer> input, Player player, Faction faction, CommandArgument argument) {
        if (faction == null || player == null) return input;
        for (ArgumentFilterType type : argument.getFilters()) {
            switch(type) {
                case ExcludeOfficers:
                    input = input.filter(p -> !faction.isOfficer(p.getUniqueId()));
                    break;
                case ExcludeSelf:
                    input = input.filter(p -> !p.equals(player));
                    break;
                case NotInExecutorsFaction:
                    input = input.filter(p -> !faction.isMember(p.getUniqueId()));
                    break;
                case NotInAnyFaction:
                    input = input.filter(p -> this.dataService.getFactionRepository().getForPlayer(p.getUniqueId()) == null);
                    break;
                default:
                    break;
            }
        }
        return input;
    }

    private String quoteifyIfNeeded(String option, CommandArgument argument) {
        if (argument.expectsDoubleQuotes()) return String.format("\"%s\"", option);
        return option;
    }

    @Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        // Bail if we have 0 arguments
        if (args.length == 0) return null;

        // Go!
        return this.handleTabCompletionForCommand(sender, new ArrayList<>(), new ArrayList<>(Arrays.asList(args)), 0);
    }
}
