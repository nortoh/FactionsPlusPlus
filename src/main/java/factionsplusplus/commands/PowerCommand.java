/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.FPPPlayer;
import factionsplusplus.services.DataService;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.UUID;

@Singleton
public class PowerCommand extends Command {

    private final DataService dataService;

    @Inject
    public PowerCommand(DataService dataService) {
        super(
            new CommandBuilder()
                .withName("power")
                .withAliases(LOCALE_PREFIX + "CmdPower")
                .withDescription("Check your power level.")
                .requiresPermissions("mf.power")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the player to check power for")
                        .expectsAnyPlayer()
                        .isOptional()
                )
        );
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        final FPPPlayer record;
        double maxPower;
        if (context.getRawArguments().length == 0) {
            if (context.isConsole()) {
                context.error("Error.PlayerExecutionRequired");
                return;
            }
            record = this.dataService.getPlayer(context.getPlayer().getUniqueId());
            context.replyWith("CommandResponse.Power.Self", record.getPower(), record.getMaxPower());
            return;
        }
        final UUID target = context.getOfflinePlayerArgument("player").getUniqueId();
        record = this.dataService.getPlayer(target);
        maxPower = record.getMaxPower();
        context.replyWith(
            "CommandResponse.Power.Other",
            context.getOfflinePlayerArgument("player").getName(),
            record.getPower(),
            maxPower
        );
    }
}