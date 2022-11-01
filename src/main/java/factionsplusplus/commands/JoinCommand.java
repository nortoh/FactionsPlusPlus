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
import factionsplusplus.services.DataService;
import factionsplusplus.utils.Logger;

import org.bukkit.Bukkit;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.events.internal.FactionJoinEvent;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class JoinCommand extends Command {
    private final Logger logger;
    private final DataService dataService;

    @Inject
    public JoinCommand(Logger logger, DataService dataService) {
        super(
            new CommandBuilder()
                .withName("join")
                .withAliases(LOCALE_PREFIX + "CmdJoin")
                .withDescription("Joins a faction.")
                .requiresPermissions("mf.join")
                .expectsPlayerExecution()
                .expectsNoFactionMembership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to join")
                        .expectsFaction()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.logger = logger;
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        if (! this.dataService.hasFactionInvite(target, context.getPlayer())) {
            context.replyWith("NotInvite");
            return;
        }
        FactionJoinEvent joinEvent = new FactionJoinEvent(target, context.getPlayer());
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            this.logger.debug("Join event was cancelled.");
            return;
        }
        context.messageFaction(
            target, 
            this.constructMessage("HasJoined")
                .with("name", context.getPlayer().getName())
                .with("faction", target.getName())
        );
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                target.upsertMember(context.getPlayer().getUniqueId(), GroupRole.Member);
                dataService.removeFactionInvite(target, context.getPlayer());
                context.replyWith(
                    constructMessage("AlertJoinedFaction")
                        .with("faction", target.getName())
                );
            }
        });
    }
}