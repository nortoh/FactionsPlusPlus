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
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.Gate;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.RelationChecker;
import dansplugins.factionsystem.utils.extended.Messenger;
import dansplugins.factionsystem.utils.extended.Scheduler;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Objects;

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
    private final Set<SubCommand> subCommands = new HashSet<>();

    @Inject
    public CommandService(LocaleService localeService, MedievalFactions medievalFactions, ConfigService configService, PlayerService playerService, MessageService messageService) {
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.configService = configService;
        this.playerService = playerService;
        this.messageService = messageService;
    }

    public void registerCommands() {
        this.subCommands.addAll(Arrays.asList(
                this.registerCommand(AddLawCommand.class),
                this.registerCommand(AllyCommand.class),
                this.medievalFactions.getInjector().getInstance(AutoClaimCommand.class),
                this.medievalFactions.getInjector().getInstance(BreakAllianceCommand.class),
                this.medievalFactions.getInjector().getInstance(BypassCommand.class),
                this.medievalFactions.getInjector().getInstance(ChatCommand.class),
                this.medievalFactions.getInjector().getInstance(CheckAccessCommand.class),
                this.medievalFactions.getInjector().getInstance(ClaimCommand.class),
                this.medievalFactions.getInjector().getInstance(ConfigCommand.class),
                this.medievalFactions.getInjector().getInstance(CreateCommand.class),
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

    public SubCommand registerCommand(Class commandClass) {
        SubCommand command = (SubCommand)this.medievalFactions.getInjector().getInstance(commandClass);
        this.loadCommandNames(command);
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
                sender.sendMessage(command.translate(this.getText("OnlyPlayersCanUseCommand")));
                return false;
            }
            Player player = (Player) sender;
            if (command.shouldRequirePlayerInFaction()) { // Find and check the status of a Faction.
                Faction faction = this.playerService.getPlayerFaction(player);
                if (faction == null) {
                    player.sendMessage(command.translate("&c" + this.getText("AlertMustBeInFactionToUseCommand")));
                    return false;
                }
                if (command.shouldRequireFactionOfficer()) { // If the command requires an Officer or higher, check for it.
                    if (!(faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId()))) {
                        player.sendMessage(command.translate("&c" + this.getText("AlertMustBeOwnerOrOfficerToUseCommand")));
                        return false;
                    }
                }
                if (command.shouldRequireFactionOwner() && !faction.isOwner(player.getUniqueId())) { // If the command requires an owner only, check for it.
                    player.sendMessage(command.translate("&c" + this.getText("AlertMustBeOwnerToUseCommand")));
                    return false;
                }
                command.setUserFaction(faction);
            }
            if (!command.checkPermissions(sender, true)) {
                // TODO: re-add missing permissions
                this.playerService.sendMessage(
                    sender,
                    command.translate("&c" + this.getText("PermissionNeeded")), 
                    Objects.requireNonNull(this.messageService.getLanguage().getString("PermissionNeeded"))
                        .replace("#permission#", ""), 
                    true
                );
                return false;
            }
        }
        return true;
    }

    /**
     * Method to obtain text from a key.
     *
     * @param key of the message in LocaleManager.
     * @return String message
     */
    protected String getText(String key) {
        String text = this.localeService.getText(key);
        return text.replace("%d", "%s");
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

            // Find the subcommand, if it exists.
            SubCommand subCommand = this.findSubCommandByName(args[0]);
            if (subCommand == null) {
                this.playerService.sendMessage(sender, ChatColor.RED + this.localeService.get("CommandNotRecognized"), "CommandNotRecognized", false);
            }
            String[] arguments = new String[args.length - 1]; // Take first argument out of Array.
            System.arraycopy(args, 1, arguments, 0, arguments.length);
            if (this.performCommandChecks(subCommand, sender, arguments, args[0])) {
                if (subCommand.shouldBePlayerCommand()) {
                    subCommand.execute((Player) sender, arguments, args[0]);
                } else {
                    subCommand.execute(sender, arguments, args[0]);
                }
            }
            return true; // Return true as the command was found and run.
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
        for (SubCommand subCommand : this.subCommands) {
            if (subCommand.checkPermissions(sender)) commandNames.add(subCommand.getPrimaryCommandName().toLowerCase());
        }
        return commandNames;
    }

    @Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
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
