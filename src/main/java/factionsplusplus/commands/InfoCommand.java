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
import factionsplusplus.services.FactionService;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class InfoCommand extends Command {
    private final FactionService factionService;

    @Inject
    public InfoCommand(FactionService factionService) {
        super(
            new CommandBuilder()
                .withName("info")
                .withAliases(LOCALE_PREFIX + "CmdInfo")
                .withDescription("See your faction or another faction's information.")
                .requiresPermissions("mf.info")
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("optional faction to get information on")
                        .expectsFaction()
                        .consumesAllLaterArguments()
                        .isOptional()
                )
        );
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        final Faction target;
        if (context.getRawArguments().length == 0) {
            if (context.isConsole()) {
                context.error("Error.PlayerExecutionRequired");
                return;
            }
            target = context.getExecutorsFaction();
            if (target == null) {
                context.error("Error.Faction.MembershipNeeded");
                return;
            }
        } else {
            target = context.getFactionArgument("faction name");
        }
        context.replyWith(
            this.factionService.generateFactionInfo(target)
        );
    }
}