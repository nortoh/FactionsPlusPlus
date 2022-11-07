/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import org.bukkit.Bukkit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class AllyCommand extends Command {

    @Inject
    public AllyCommand() {
        super(
            new CommandBuilder()
                .withName("ally")
                .withAliases(LOCALE_PREFIX + "CmdAlly")
                .withDescription("Attempt to ally with a faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.ally")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to request as an ally")
                        .expectsFaction()
                        .addFilters(ArgumentFilterType.NotAllied, ArgumentFilterType.NotOwnFaction)
                        .consumesAllLaterArguments()
                        .isRequired() 
                )
        );
    }

    public void execute(CommandContext context) {
        // retrieve the Faction from the given arguments
        final Faction otherFaction = context.getFactionArgument("faction name");

        // the faction can't be itself
        if (otherFaction == context.getExecutorsFaction()) {
            context.error("Error.Alliance.Self");
            return;
        }

        // no need to allow them to ally if they're already allies
        if (context.getExecutorsFaction().isAlly(otherFaction.getUUID())) {
            context.error("Error.Alliance.Exists", otherFaction.getName());
            return;
        }

        if (context.getExecutorsFaction().isEnemy(otherFaction.getUUID())) {
            context.error("Error.Alliance.Enemy", otherFaction.getName());
            return;
        }

        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getUUID())) {
            context.error("Error.Alliance.AlreadyRequested", otherFaction.getName());
            return;
        }

        // send the request
        context.getExecutorsFaction().requestAlly(otherFaction.getID());

        context.getExecutorsFaction().alert("FactionNotice.AllianceRequest.Source", otherFaction.getName());
        otherFaction.alert("FactionNotice.AllianceRequest.Target", context.getExecutorsFaction().getName());

        // check if both factions have requested an alliance
        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getUUID()) && otherFaction.isRequestedAlly(context.getExecutorsFaction().getUUID())) {
            Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    // Running this against one faction will trickle down to the other.
                    context.getExecutorsFaction().upsertRelation(otherFaction.getUUID(), FactionRelationType.Ally);
                    // message player's faction
                    context.getExecutorsFaction().alert("FactionNotice.Allied", otherFaction.getName());
                    // message target faction
                    otherFaction.alert("FactionNotice.Allied", context.getExecutorsFaction().getName());
                    // remove alliance requests
                    context.getExecutorsFaction().removeAllianceRequest(otherFaction.getUUID());
                    otherFaction.removeAllianceRequest(context.getExecutorsFaction().getUUID());
                }
            });
        }
    }
}