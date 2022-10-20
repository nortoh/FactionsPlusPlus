/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.commands.*;
import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionFlag;
import factionsplusplus.utils.Logger;
import factionsplusplus.utils.RelationChecker;
import factionsplusplus.utils.StringUtils;
import factionsplusplus.utils.extended.Messenger;
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
import factionsplusplus.repositories.CommandRepository;

import factionsplusplus.builders.MessageBuilder;

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

    @Inject
    public CommandService(LocaleService localeService, FactionsPlusPlus factionsPlusPlus, ConfigService configService, PlayerService playerService, MessageService messageService, CommandRepository commandRepository, DataService dataService) {
        this.localeService = localeService;
        this.factionsPlusPlus = factionsPlusPlus;
        this.configService = configService;
        this.playerService = playerService;
        this.messageService = messageService;
        this.commandRepository = commandRepository;
        this.dataService = dataService;
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
            playerFaction = this.playerService.getPlayerFaction(sender);
            context.setExecutorsFaction(playerFaction);
        }
        if (command.shouldRequireFactionMembership()) {
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
            Command newCommand = command.getSubCommand(arguments.get(0));
            if (newCommand == null) {
                // If a subcommand is required, we bail now.
                if (command.shouldRequireSubCommand()) {
                    this.messageService.sendInvalidSyntaxMessage(sender, command.getName(), command.buildSyntax());
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
            // TODO: handle errors
            if (arguments.size() > 0) {
                String argumentData = arguments.remove(0);
                // Check if the argument should start and end with double quotes, if so, append all other arguments until we get another double quote
                if (argumentData.startsWith("\"") && argument.expectsDoubleQuotes()) {
                    Boolean foundEnd = false;
                    // Remove the opening quote
                    argumentData = argumentData.substring(1);
                    while (arguments.size() > 0) {
                        if (arguments.get(0).startsWith("\"")) foundEnd = true;
                        argumentData = argumentData + " " + arguments.remove(0);
                    }
                    // Remove the closing quote
                    argumentData = argumentData.substring(0, argumentData.length() - 1);
                    if (!foundEnd) {
                        this.messageService.sendInvalidSyntaxMessage(sender, context.getCommandNames(), command.buildSyntax());
                        return false;
                    }
                }
                Object parsedArgumentData = null;
                Faction faction = null;
                OfflinePlayer player = null;
                switch(argument.getType()) {
                    case Faction:
                        faction = this.getAnyFaction(context, argumentData);
                        if (faction != null) break;
                        return false;
                    case ConfigOptionName:
                        if (this.configService.getConfigOption(argumentData) != null) {
                            parsedArgumentData = argumentData;
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
                        parsedArgumentData = Integer.parseInt(argumentData);
                        break;
                    case Double:
                        parsedArgumentData = Double.parseDouble(argumentData);
                        break;
                    default:
                        parsedArgumentData = argumentData;
                        break;
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

    public FactionFlag getFactionFlag(CommandContext context, String argumentData) {
        final FactionFlag flag = context.getExecutorsFaction().getFlag(argumentData);
        if (flag == null) {
            context.replyWith(
                new MessageBuilder("InvalidFactionFlag")
                    .with("flag", argumentData)
            );
        }
        return flag;
    }
    public Faction getAnyFaction(CommandContext context, String argumentData) {
        final Faction faction = this.dataService.getFaction(argumentData);
        if (faction == null) {
            context.replyWith(
                new MessageBuilder("FactionNotFound")
                    .with("faction", argumentData)
            );
        }
        return faction;
    }

    public Faction getAllyFaction(CommandContext context, String argumentData) {
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

    public Faction getEnemyFaction(CommandContext context, String argumentData) {
        final Faction faction = this.getAnyFaction(context, argumentData);
        if (faction != null) {
            if (context.getExecutorsFaction().isEnemy(faction.getID())) {
                return faction;
            }
            context.replyWith("FactionNotEnemy");
        }
        return null;
    }

    public Faction getVassaledFaction(CommandContext context, String argumentData) {
        final Faction faction = this.getAnyFaction(context, argumentData);
        if (faction != null) {
            if (faction.isLiege(context.getExecutorsFaction().getID())) {
                return faction;
            }
            context.replyWith("FactionIsNotVassal");
        }
        return null;
    }

    public OfflinePlayer getPlayer(CommandContext context, String argumentData) {
        final OfflinePlayer player = StringUtils.parseAsPlayer(argumentData);
        if (player == null) {
            context.replyWith(
                new MessageBuilder("PlayerNotFound")
                    .with("name", argumentData)
            );
        }
        return player;
    }

    public OfflinePlayer getFactionMember(CommandContext context, String argumentData) {
        final OfflinePlayer player = this.getPlayer(context, argumentData);
        if (player != null) {
            if (context.getExecutorsFaction().isMember(player.getUniqueId())) {
                return player;
            }
            context.replyWith("PlayerIsNotMemberOfFaction");
        }
        return null;
    }

    public OfflinePlayer getFactionOfficer(CommandContext context, String argumentData) {
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
        // mf commands
        if (label.equalsIgnoreCase("mf")) {

            // no arguments check
            if (args.length == 0) {
                this.localeService.getStrings("PluginInfo")
                    .forEach(s -> {
                        s = s.replace("#version", this.factionsPlusPlus.getVersion()).replace("#dev", this.factionsPlusPlus.getDescription().getAuthors().toString());
                        this.messageService.send(sender, s);
                    });
                return true;
            }

            // Convert arguments to a stack
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
        return false;
    }

    private ArrayList<String> getSubCommandNamesForSender(CommandSender sender) {
        ArrayList<String> commandNames = new ArrayList<String>();
        for (Command subCommand : this.commandRepository.all().values()) {
            if (this.checkPermissions(sender, subCommand.getRequiredPermissions()).size() == 0) commandNames.add(subCommand.getName().toLowerCase());
        }
        return commandNames;
    }

    @Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        List<String> result = new ArrayList<String>();

        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Auto-complete subcommands
            if (args.length == 1) {
                ArrayList<String> accessibleCommands = this.getSubCommandNamesForSender(sender);
                for (String commandName : accessibleCommands) {
                    if (commandName.startsWith(args[0].toLowerCase())) result.add(commandName);
                }
                return result;
            } else {
                // Attempt to find subcommand based on first argument
                Command subCommand = this.commandRepository.get(args[0]);
                // Bail if no command found (can't autocomplete something we don't know about)
                if (subCommand == null) {
                    return null;
                }
                // Check if there's any subcommands we can tab complete
                Boolean hadSubCommand = false;
                if (subCommand.hasSubCommands()) {
                    for (String commandName : subCommand.getSubCommands().keySet()) {
                        if (commandName.startsWith(args[1].toLowerCase())) {
                            result.add(commandName);
                            hadSubCommand = true;
                        }
                    }
                    return result;
                }
                // Pass response to subcommand handler
                String[] arguments = new String[args.length - (hadSubCommand ? 2 : 1)]; // Take first argument out of Array.
                System.arraycopy(args, (hadSubCommand ? 2 : 1), arguments, 0, arguments.length);
                return subCommand.onTabComplete(sender, arguments);
            }
        }
        return null;
    }
}