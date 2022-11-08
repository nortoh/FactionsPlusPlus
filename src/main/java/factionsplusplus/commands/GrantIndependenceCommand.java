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

@Singleton
public class GrantIndependenceCommand extends Command {

    @Inject
    public GrantIndependenceCommand() {
        super(
            new CommandBuilder()
                .withName("grantindependence")
                .withAliases("gi", LOCALE_PREFIX + "CmdGrandIndependence")
                .withDescription("Grants independence to a vassaled faction.")
                .requiresPermissions("mf.grantindependence")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to get a members list of")
                        .expectsVassaledFaction()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        context.getExecutorsFaction().upsertRelation(target.getUUID(), null);
        // inform all players in that faction that they are now independent
        target.alert("FactionNotice.Independence", context.getExecutorsFaction().getName());
        // inform all players in players faction that a vassal was granted independence
        context.getExecutorsFaction().alert("FactionNotice.VassalRemoved", target.getName());
    }
}