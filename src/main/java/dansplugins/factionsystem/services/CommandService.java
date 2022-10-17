/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.*;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.RelationChecker;
import dansplugins.factionsystem.utils.extended.Messenger;
import dansplugins.factionsystem.utils.extended.Scheduler;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandArgument;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.repositories.CommandRepository;

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
    private final MedievalFactions medievalFactions;
    private final ConfigService configService;
    private final PlayerService playerService;
    private final MessageService messageService;
    private final CommandRepository commandRepository;
    private final DataService dataService;
    private final Set<SubCommand> subCommands = new HashSet<>();

    @Inject
    public CommandService(LocaleService localeService, MedievalFactions medievalFactions, ConfigService configService, PlayerService playerService, MessageService messageService, CommandRepository commandRepository, DataService dataService) {
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.configService = configService;
        this.playerService = playerService;
        this.messageService = messageService;
        this.commandRepository = commandRepository;
        this.dataService = dataService;
    }

    public void registerCommands() {
        this.registerCommand(AddLawCommand.class);
        this.registerCommand(CreateCommand.class);
        this.registerCommand(AutoClaimCommand.class);
        this.subCommands.addAll(Arrays.asList(
                //this.registerCommand(AllyCommand.class),
                this.medievalFactions.getInjector().getInstance(BreakAllianceCommand.class),
                this.medievalFactions.getInjector().getInstance(BypassCommand.class),
                this.medievalFactions.getInjector().getInstance(ChatCommand.class),
                this.medievalFactions.getInjector().getInstance(CheckAccessCommand.class),
                this.medievalFactions.getInjector().getInstance(ClaimCommand.class),
                this.medievalFactions.getInjector().getInstance(ConfigCommand.class),
                this.medievalFactions.getInjector().getInstance(DeclareIndependenceCommand.class),
                this.medievalFactions.getInjector().getInstance(DeclareWarCommand.class),
                this.medievalFactions.getInjector().getInstance(DemoteCommand.class),
                this.medievalFactions.getInjector().getInstance(DescCommand.class),
                this.medievalFactions.getInjector().getInstance(DisbandCommand.class),
                this.medievalFactions.getInjector().getInstance(DuelCommand.class),
                this.medievalFactions.getInjector().getInstance(EditLawCommand.class),
                this.medievalFactions.getInjector().getInstance(FlagsCommand.class),
                this.medievalFactions.getInjector().getInstance(ForceCommand.class),
                this.medievalFactions.getInjector().getInstance(GateCommand.class),
                this.medievalFactions.getInjector().getInstance(GrantAccessCommand.class),
                this.medievalFactions.getInjector().getInstance(GrantIndependenceCommand.class),
                this.medievalFactions.getInjector().getInstance(HelpCommand.class),
                this.medievalFactions.getInjector().getInstance(HomeCommand.class),
                this.medievalFactions.getInjector().getInstance(InfoCommand.class),
                this.medievalFactions.getInjector().getInstance(InviteCommand.class),
                this.medievalFactions.getInjector().getInstance(InvokeCommand.class),
                this.medievalFactions.getInjector().getInstance(JoinCommand.class),
                this.medievalFactions.getInjector().getInstance(KickCommand.class),
                this.medievalFactions.getInjector().getInstance(LawsCommand.class),
                this.medievalFactions.getInjector().getInstance(LeaveCommand.class),
                this.medievalFactions.getInjector().getInstance(ListCommand.class),
                this.medievalFactions.getInjector().getInstance(LockCommand.class),
                this.medievalFactions.getInjector().getInstance(MakePeaceCommand.class),
                this.medievalFactions.getInjector().getInstance(MembersCommand.class),
                this.medievalFactions.getInjector().getInstance(PowerCommand.class),
                this.medievalFactions.getInjector().getInstance(PrefixCommand.class),
                this.medievalFactions.getInjector().getInstance(PromoteCommand.class),
                this.medievalFactions.getInjector().getInstance(RemoveLawCommand.class),
                this.medievalFactions.getInjector().getInstance(RenameCommand.class),
                this.medievalFactions.getInjector().getInstance(ResetPowerLevelsCommand.class),
                this.medievalFactions.getInjector().getInstance(RevokeAccessCommand.class),
                this.medievalFactions.getInjector().getInstance(SetHomeCommand.class),
                this.medievalFactions.getInjector().getInstance(SwearFealtyCommand.class),
                this.medievalFactions.getInjector().getInstance(TransferCommand.class),
                this.medievalFactions.getInjector().getInstance(UnclaimallCommand.class),
                this.medievalFactions.getInjector().getInstance(UnclaimCommand.class),
                this.medievalFactions.getInjector().getInstance(UnlockCommand.class),
                this.medievalFactions.getInjector().getInstance(VassalizeCommand.class),
                this.medievalFactions.getInjector().getInstance(VersionCommand.class),
                this.medievalFactions.getInjector().getInstance(WhoCommand.class),
                this.medievalFactions.getInjector().getInstance(MapCommand.class),
                this.medievalFactions.getInjector().getInstance(StatsCommand.class)
        ));
    }

    public Command registerCommand(Class commandClass) {
        Command command = (Command)this.medievalFactions.getInjector().getInstance(commandClass);
        this.commandRepository.add(command);
        return command;
    }

    public void loadCommandNames(SubCommand command) {
        String[] rawNames = command.getCommandNames();
        for (int i = 0; i < rawNames.length; i++) {
            String name = rawNames[i];
            if (name.contains(SubCommand.LOCALE_PREFIX)) name = this.localeService.getText(name.replace(SubCommand.LOCALE_PREFIX, ""));
            command.setName(i, name);
        }
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

    /**
     * Method to be called by the command interpreter <em>only</em>.
     * <p>
     * This method uses the in-class variables to call a different method based on the parameters specified.
     * <br>For example, if {@link SubCommand#playerCommand} is {@code true},
     * <br>{@link SubCommand#execute(Player, String[], String)} is executed,
     * <br>not {@link SubCommand#execute(CommandSender, String[], String)}.
     * </p>
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command.
     */
    public boolean performCommandChecks(SubCommand command, CommandSender sender, String[] args, String key) {
        if (command.shouldBePlayerCommand()) {
            if (!(sender instanceof Player)) { // Require a player for a player-only command.
                sender.sendMessage(command.translate(this.localeService.getText("OnlyPlayersCanUseCommand")));
                return false;
            }
            Player player = (Player) sender;
            if (command.shouldRequirePlayerInFaction()) { // Find and check the status of a Faction.
                Faction faction = this.playerService.getPlayerFaction(player);
                if (faction == null) {
                    player.sendMessage(command.translate("&c" + this.localeService.getText("AlertMustBeInFactionToUseCommand")));
                    return false;
                }
                if (command.shouldRequireFactionOfficer()) { // If the command requires an Officer or higher, check for it.
                    if (!(faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId()))) {
                        player.sendMessage(command.translate("&c" + this.localeService.getText("AlertMustBeOwnerOrOfficerToUseCommand")));
                        return false;
                    }
                }
                if (command.shouldRequireFactionOwner() && !faction.isOwner(player.getUniqueId())) { // If the command requires an owner only, check for it.
                    player.sendMessage(command.translate("&c" + this.localeService.getText("AlertMustBeOwnerToUseCommand")));
                    return false;
                }
                command.setUserFaction(faction);
            }
            List<String> missingPermissions = command.checkPermissions(sender, true);
            if (missingPermissions.size() > 0) {
                this.messageService.sendPermissionMissingMessage(sender, missingPermissions);
                return false;
            }
        }
        return true;
    }

    public boolean processCommand(Command parentCommand, Command command, CommandSender sender, ArrayList<String> arguments, CommandContext context) {
        // If context is null, create a new command context. This usually means we're a root command.
        if (context == null) {
            context = new CommandContext();
            context.setSender(sender);
            context.setRawArguments((String[])arguments.toArray());
        }

        // If arguments are missing, let the user know.
        if (command.getRequiredArgumentCount() > arguments.size()) {
            this.messageService.sendInvalidSyntaxMessage(sender, command.getName(), command.buildSyntax());
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
                this.messageService.sendOnlyPlayersCanUseThisCommandMessage(sender);
                return false;
            }
        }

        // Check if this command should require faction specific stuff
        Faction playerFaction = this.playerService.getPlayerFaction(sender);
        context.setExecutorsFaction(playerFaction);
        if (command.shouldRequireFactionMembership()) {
            if (playerFaction == null) {
                this.messageService.sendFactionMembershipRequiredMessage(sender);
                return false;
            }
        }

        // Faction owner?
        if (command.shouldRequireFactionOwnership()) {
            if (context.isConsole()) return false; // bail if console
            if (! playerFaction.getOwner().equals(((Player)sender).getUniqueId())) {
                this.messageService.sendFactionOwnershipRequiredMessage(sender);
                return false;
            }
        }

        // Faction officer or owner
        if (command.shouldRequireFactionOfficership()) {
            if (context.isConsole()) return false; // bail if console
            UUID senderUUID = ((Player)sender).getUniqueId();
            if (! playerFaction.getOwner().equals(senderUUID) && ! playerFaction.isOfficer(senderUUID)) {
                this.messageService.sendFactionOwnershipOrOfficershipRequiredMessage(sender);
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
            if (argument.shouldConsumeAllArguments()) {
                context.addArgument(argumentName, String.join(" ", arguments));
                break;
            }

            // Pop the next argument (what we should be using)
            // TODO: handle errors
            if (arguments.size() > 0) {
                String argumentData = arguments.remove(0);
                Object parsedArgumentData = null;
                switch(argument.getType()) {
                    case Faction:
                        parsedArgumentData = this.dataService.getFaction(argumentData);
                        break;
                    case Integer:
                        parsedArgumentData = Integer.getInteger(argumentData);
                        break;
                    case Double:
                        parsedArgumentData = Double.parseDouble(argumentData);
                        break;
                    default:
                        parsedArgumentData = argumentData;
                        break;
                }
                context.addArgument(argumentName, parsedArgumentData);
            } else {
                break;
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

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        // mf commands
        if (label.equalsIgnoreCase("mf")) {

            // no arguments check
            if (args.length == 0) {
                // send plugin information
                if (!this.configService.getBoolean("useNewLanguageFile")) {
                    sender.sendMessage(ChatColor.AQUA + String.format(this.localeService.get("MedievalFactionsTitle"), this.medievalFactions.getVersion()));
                    sender.sendMessage(ChatColor.AQUA + String.format(this.localeService.get("DeveloperList"), this.medievalFactions.getDescription().getAuthors()));
                    sender.sendMessage(ChatColor.AQUA + this.localeService.get("WikiLink"));
                    sender.sendMessage(ChatColor.AQUA + String.format(this.localeService.get("CurrentLanguageID"), this.configService.getString("languageid")));
                    sender.sendMessage(ChatColor.AQUA + String.format(this.localeService.get("SupportedLanguageIDList"), this.localeService.getSupportedLanguageIDsSeparatedByCommas()));
                } else {
                    this.messageService.getLanguage().getStringList("PluginInfo")
                            .forEach(s -> {
                                s = s.replace("#version#", this.medievalFactions.getVersion()).replace("#dev#", this.medievalFactions.getDescription().getAuthors().toString());
                                this.playerService.sendMessage(sender, s, s, true);
                            });
                }
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
                this.messageService.sendCommandNotFoundMessage(sender);
                return false;
            }

            return this.processCommand(command, command, sender, argumentList, null);

        }
        return false;
    }

    private SubCommand findSubCommandByName(String name) {
        for (SubCommand subCommand : this.subCommands) {
            if (subCommand.isCommand(name)) {
                return subCommand;
            }
        }
        return null;
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
                SubCommand subCommand = this.findSubCommandByName(args[0]);
                // Bail if no command found (can't autocomplete something we don't know about)
                if (subCommand == null) {
                    return null;
                }
                // Pass response to subcommand handler
                String[] arguments = new String[args.length - 1]; // Take first argument out of Array.
                System.arraycopy(args, 1, arguments, 0, arguments.length);
                return subCommand.onTabComplete(sender, arguments);
            }
        }
        return null;
    }
}
