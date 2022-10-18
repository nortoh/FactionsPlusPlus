/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import dansplugins.factionsystem.builders.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class MembersCommand extends Command {

    private final PlayerService playerService;
    private final MessageService messageService;
    private final LocaleService localeService;
    private final ConfigService configService;
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    @Inject
    public MembersCommand(
        PlayerService playerService,
        MessageService messageService,
        LocaleService localeService,
        ConfigService configService,
        PersistentData persistentData,
        FactionRepository factionRepository
    ) {
        super(
            new CommandBuilder()
                .withName("members")
                .withAliases(LOCALE_PREFIX + "CmdMembers")
                .withDescription("List the members of your faction or another faction.")
                .requiresPermissions("mf.members")
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to get a members list of")
                        .expectsFaction()
                )
        );
        this.playerService = playerService;
        this.messageService = messageService;
        this.localeService = localeService;
        this.configService = configService;
        this.persistentData = persistentData;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        final Faction faction;
        CommandSender sender = context.getSender();
        if (context.getRawArguments().length == 0) {
            if (context.isConsole()) {
                context.replyWith("OnlyPlayersCanUseCommand");
                return;
            }
            faction = context.getExecutorsFaction();
            if (faction == null) {
                context.replyWith("AlertMustBeInFactionToUseCommand");
                return;
            }
        } else {
            faction = context.getFactionArgument("faction name");
        }
        // send Faction Members
        if (!this.configService.getBoolean("useNewLanguageFile")) {
            sender.sendMessage(this.translate("&b----------\n" + this.localeService.getText("MembersOf", faction.getName())));
            sender.sendMessage(this.translate("&b----------\n"));
            faction.getMemberList().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(player -> {
                        String rank = "";
                        String color = "&a";
                        if (faction.isOfficer(player.getUniqueId())) {
                            rank = "*";
                            color = "&b";
                        }
                        if (faction.isOwner(player.getUniqueId())) {
                            rank = "**";
                            color = "&c";
                        }
                        return this.translate("&f" + player.getName() + color + rank);
                    }).forEach(sender::sendMessage);
            sender.sendMessage(this.translate("&b----------\n"));
        } else {
            this.playerService.sendMessage(
                sender,
                "",
                Objects.requireNonNull(this.messageService.getLanguage().getString("MembersFaction.Title")).replace("#faction#", faction.getName()),
                true
            );
            faction.getMemberList().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(player -> {
                        String rank = this.messageService.getLanguage().getString("MembersFaction.Member.Rank");
                        String color = this.messageService.getLanguage().getString("MembersFaction.Member.Color");
                        if (faction.isOfficer(player.getUniqueId())) {
                            rank = this.messageService.getLanguage().getString("MembersFaction.Officer.Rank");
                            color = this.messageService.getLanguage().getString("MembersFaction.Officer.Color");
                        }
                        if (faction.isOwner(player.getUniqueId())) {
                            rank = this.messageService.getLanguage().getString("MembersFaction.Owner.Rank");
                            color = this.messageService.getLanguage().getString("MembersFaction.Owner.Color");
                        }
                        return this.playerService.colorize(Objects.requireNonNull(this.messageService.getLanguage().getString("MembersFaction.Message"))
                                .replace("#color#", Objects.requireNonNull(color))
                                .replace("#rank#", Objects.requireNonNull(rank))
                                .replace("#name#", Objects.requireNonNull(player.getName())));
                    }).forEach(sender::sendMessage);
            this.playerService.sendMessage(
                sender,
                "",
                Objects.requireNonNull(this.messageService.getLanguage().getString("MembersFaction.SubTitle")).replace("#faction#", faction.getName()),
                true
            );

        }
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}