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

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class SwearFealtyCommand extends Command {

    @Inject
    public SwearFealtyCommand() {
        super(
            new CommandBuilder()
                .withName("swearfelty")
                .withAliases("sf", LOCALE_PREFIX + "CmdSwearFealty")
                .withDescription("Swear fealty to a faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.swearfealty")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to swear fealty to")
                        .expectsFaction()
                        .addFilters(ArgumentFilterType.OfferedVassalization)
                        .consumesAllLaterArguments()
                        .isRequired() 
                )
        );
    }

    public void execute(CommandContext context) {
        final Faction faction = context.getExecutorsFaction();
        final Faction target = context.getFactionArgument("faction name");
        if (! target.hasBeenOfferedVassalization(faction.getID())) {
            context.replyWith("AlertNotOfferedVassalizationBy");
            return;
        }
        // set vassal
        target.addVassal(faction.getID());
        target.removeAttemptedVassalization(faction.getID());

        // set liege
        faction.setLiege(target.getID());

        
        // inform target faction that they have a new vassal
        context.messageFaction(
            target,
            this.constructMessage("AlertFactionHasNewVassal")
                .with("name", faction.getName())
        );
        // inform players faction that they have a new liege
        context.messagePlayersFaction(
            this.constructMessage("AlertFactionHasBeenVassalized")
                .with("name", target.getName())
        );
    }
}