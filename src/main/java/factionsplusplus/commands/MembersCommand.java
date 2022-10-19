/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.MedievalFactions;
import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.PlayerService;
import factionsplusplus.utils.TabCompleteTools;
import factionsplusplus.builders.*;
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
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    @Inject
    public MembersCommand(
        PlayerService playerService,
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
                        .consumesAllLaterArguments()
                        .isOptional()
                )
        );
        this.playerService = playerService;
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
        context.replyWith(
            this.constructMessage("MembersFaction.Title")
                .with("faction", faction.getName())
        );
        faction.getMembers().stream()
                .map(Bukkit::getOfflinePlayer)
                .forEach(player -> {
                    String rank = context.getLocalizedString("MembersFaction.Member.Rank");
                    String color = context.getLocalizedString("MembersFaction.Member.Color");
                    if (faction.isOfficer(player.getUniqueId())) {
                        rank = context.getLocalizedString("MembersFaction.Officer.Rank");
                        color = context.getLocalizedString("MembersFaction.Officer.Color");
                    }
                    if (faction.isOwner(player.getUniqueId())) {
                        rank = context.getLocalizedString("MembersFaction.Owner.Rank");
                        color = context.getLocalizedString("MembersFaction.Owner.Color");
                    }
                    context.replyWith(
                        this.constructMessage("MembersFaction.Message")
                            .with("color", color)
                            .with("rank", rank)
                            .with("name", player.getName())
                    );
                });
        context.replyWith(
            this.constructMessage("MembersFaction.SubTitle")
                .with("faction", faction.getName())
        );
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