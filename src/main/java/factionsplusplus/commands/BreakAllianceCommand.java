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
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class BreakAllianceCommand extends Command {

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
            context.error("Error.BreakAlliance.Self");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                context.getExecutorsFaction().clearRelation(otherFaction.getID());
                context.getExecutorsFaction().alert("FactionNotice.AllianceBroken.Source", otherFaction.getName());
                otherFaction.alert("FactionNotice.AllianceBroken.Target", context.getExecutorsFaction().getName());
            }
        });
    }
}