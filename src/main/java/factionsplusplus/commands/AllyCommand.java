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
import factionsplusplus.services.LocaleService;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class AllyCommand extends Command {
    protected final LocaleService localeService;
    protected final FactionRepository factionRepository;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public AllyCommand(
        LocaleService localeService,
        FactionRepository factionRepository
    ) {
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
        this.localeService = localeService;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        // retrieve the Faction from the given arguments
        final Faction otherFaction = context.getFactionArgument("faction name");

        // the faction can't be itself
        if (otherFaction == context.getExecutorsFaction()) {
            context.replyWith("CannotAllyWithSelf");
            return;
        }

        // no need to allow them to ally if they're already allies
        if (context.getExecutorsFaction().isAlly(otherFaction.getID())) {
            context.replyWith("FactionAlreadyAlly");
            return;
        }

        if (context.getExecutorsFaction().isEnemy(otherFaction.getID())) {
            context.replyWith("FactionIsEnemy");
            return;
        }

        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getID())) {
            context.replyWith("AlertAlreadyRequestedAlliance");
            return;
        }

        // send the request
        context.getExecutorsFaction().requestAlly(otherFaction.getID());

        context.messagePlayersFaction(
            this.constructMessage("AlertAttemptedAlliance")
                .with("faction_a", context.getExecutorsFaction().getName())
                .with("faction_b", otherFaction.getName())
        );

        context.messageFaction(
            otherFaction,
            this.constructMessage("AlertAttemptedAlliance")
                .with("faction_a", otherFaction.getName())
                .with("faction_b", context.getExecutorsFaction().getName())
        );

        // check if both factions have requested an alliance
        if (context.getExecutorsFaction().isRequestedAlly(otherFaction.getID()) && otherFaction.isRequestedAlly(context.getExecutorsFaction().getID())) {
            // ally them
            context.getExecutorsFaction().addAlly(otherFaction.getID());
            otherFaction.addAlly(context.getExecutorsFaction().getID());
            // message player's faction
            context.messagePlayersFaction(
                this.constructMessage("AlertNowAlliedWith")
                    .with("faction", otherFaction.getName())
            );

            // message target faction
            context.messageFaction(
                otherFaction, 
                this.constructMessage("AlertNowAlliedWith")
                    .with("faction", context.getExecutorsFaction().getName())
            );

            // remove alliance requests
            context.getExecutorsFaction().removeAllianceRequest(otherFaction.getID());
            otherFaction.removeAllianceRequest(context.getExecutorsFaction().getID());
        }
    }
}