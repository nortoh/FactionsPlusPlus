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
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class BreakAllianceCommand extends Command {

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public BreakAllianceCommand() {
        super(
            new CommandBuilder()
                .withName("breakalliance")
                .withAliases("ba", LOCALE_PREFIX + "CmdBreakAlliance")
                .withDescription("Breaks an alliance with an allied faction.")
                .requiresPermissions("mf.breakalliance")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the allied faction to break alliance with")
                        .expectsAlliedFaction()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
    }


    public void execute(CommandContext context) {
        final Faction otherFaction = context.getFactionArgument("faction name");

        if (otherFaction == context.getExecutorsFaction()) {
            context.replyWith("CannotBreakAllianceWithSelf");
            return;
        }

        context.getExecutorsFaction().removeAlly(otherFaction.getID());
        otherFaction.removeAlly(context.getExecutorsFaction().getID());
        context.messagePlayersFaction(
            this.constructMessage("AllianceBrokenWith")
                .with("faction", otherFaction.getName())
        );
        context.messageFaction(
            otherFaction, 
            this.constructMessage("AlertAllianceHasBeenBroken")
                .with("faction", context.getExecutorsFaction().getName())
        );
    }
}