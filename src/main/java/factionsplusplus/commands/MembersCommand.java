/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.PlayerService;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;
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
    private final FactionRepository factionRepository;

    @Inject
    public MembersCommand(
        PlayerService playerService,
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
                    String rank = context.getLocalizedString("MembersFaction.Rank.Member.Rank");
                    String color = context.getLocalizedString("MembersFaction.Rank.Member.Color");
                    if (faction.isOfficer(player.getUniqueId())) {
                        rank = context.getLocalizedString("MembersFaction.Rank.Officer.Rank");
                        color = context.getLocalizedString("MembersFaction.Rank.Officer.Color");
                    }
                    if (faction.isOwner(player.getUniqueId())) {
                        rank = context.getLocalizedString("MembersFaction.Rank.Owner.Rank");
                        color = context.getLocalizedString("MembersFaction.Rank.Owner.Color");
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
}