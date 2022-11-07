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
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class VassalizeCommand extends Command {

    private final Logger logger;
    
    @Inject
    public VassalizeCommand(Logger logger) {
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
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        // make sure player isn't trying to vassalize their own faction
        if (context.getExecutorsFaction().getUUID().equals(target.getUUID())) {
            context.error("Error.Vassalization.Self");
            return;
        }
        // make sure player isn't trying to vassalize their liege
        if (target.equals(context.getExecutorsFaction().getLiege())) {
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
        context.getExecutorsFaction().addAttemptedVassalization(target.getUUID());

        // inform all players in that faction that they are trying to be vassalized
        target.alert("FactionNotice.VassalizationAttempted.Target", context.getExecutorsFaction().getName());

        // inform all players in players faction that a vassalization offer was sent
        context.getExecutorsFaction().alert("FactionNotice.VassalizationAttempted.Source", target.getName());
    }

    private int willVassalizationResultInLoop(Faction vassalizer, Faction potentialVassal) {
        final int MAX_STEPS = 1000;
        Faction current = vassalizer;
        int steps = 0;
        while (current != null && steps < MAX_STEPS) { // Prevents infinite loop and NPE (getFaction can return null).
            Faction liege = current.getLiege();
            if (liege == null) return 0;
            if (liege.getUUID().equals(potentialVassal.getUUID())) return 1;
            current = liege;
            steps++;
        }
        return 2; // We don't know :/
    }
}