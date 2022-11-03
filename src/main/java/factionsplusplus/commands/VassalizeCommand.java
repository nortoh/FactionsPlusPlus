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
import factionsplusplus.utils.Logger;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.data.repositories.FactionRepository;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.UUID;

@Singleton
public class VassalizeCommand extends Command {

    private final Logger logger;
    private final FactionRepository factionRepository;

    
    @Inject
    public VassalizeCommand(
        FactionRepository factionRepository,
        Logger logger
    ) {
        super(
            new CommandBuilder()
                .withName("vassalize")
                .withAliases(LOCALE_PREFIX + "CmdVassalize")
                .withDescription("Offer to vassalize a faction.")
                .requiresPermissions("mf.vassalize")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to attempt vassalization")
                        .expectsFaction()
                        .addFilters(ArgumentFilterType.NotVassal, ArgumentFilterType.NotOwnFaction)
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.factionRepository = factionRepository;
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        // make sure player isn't trying to vassalize their own faction
        if (context.getExecutorsFaction().getID().equals(target.getID())) {
            context.error("Error.Vassalization.Self");
            return;
        }
        // make sure player isn't trying to vassalize their liege
        if (target.getID().equals(context.getExecutorsFaction().getLiege())) {
            context.error("Error.Vassalization.Liege");
            return;
        }
        // make sure player isn't trying to vassalize a vassal
        if (target.hasLiege()) {
            context.error("Error.Vassalization.Vassaled");
            return;
        }
        // make sure this vassalization won't result in a vassalization loop
        final int loopCheck = this.willVassalizationResultInLoop(context.getExecutorsFaction(), target);
        if (loopCheck == 1 || loopCheck == 2) {
            this.logger.debug("Vassalization was cancelled due to potential loop");
            return;
        }
        // add faction to attemptedVassalizations
        context.getExecutorsFaction().addAttemptedVassalization(target.getID());

        // TODO: localize
        // inform all players in that faction that they are trying to be vassalized
        context.messageFaction(
            target,
            this.constructMessage("AlertAttemptedVassalization")
                .with("name", context.getExecutorsFaction().getName())
        );

        // TODO: localize
        // inform all players in players faction that a vassalization offer was sent
        context.messagePlayersFaction(
            this.constructMessage("AlertFactionAttemptedToVassalize")
                .with("name", target.getName())
        );
    }

    private int willVassalizationResultInLoop(Faction vassalizer, Faction potentialVassal) {
        final int MAX_STEPS = 1000;
        Faction current = vassalizer;
        int steps = 0;
        while (current != null && steps < MAX_STEPS) { // Prevents infinite loop and NPE (getFaction can return null).
            UUID liegeID = current.getLiege();
            if (liegeID == null) return 0;
            if (liegeID.equals(potentialVassal.getID())) return 1;
            current = this.factionRepository.get(liegeID);
            steps++;
        }
        return 2; // We don't know :/
    }
}