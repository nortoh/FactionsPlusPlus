/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.DataService;
import factionsplusplus.services.PlayerService;
import org.bukkit.command.CommandSender;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class PowerCommand extends Command {

    private final PlayerService playerService;
    private final DataService dataService;

    @Inject
    public PowerCommand(
        PlayerService playerService,
        DataService dataService
    ) {
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
        this.playerService = playerService;
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        final PlayerRecord record;
        double maxPower;
        if (context.getRawArguments().length == 0) {
            if (context.isConsole()) {
                context.replyWith("OnlyPlayersCanUseCommand");
                return;
            }
            record = this.dataService.getPlayerRecord(context.getPlayer().getUniqueId());
            maxPower = this.playerService.getMaxPower(context.getPlayer().getUniqueId());
            context.replyWith(
                this.constructMessage("AlertCurrentPowerLevel")
                    .with("power", String.valueOf(record.getPower()))
                    .with("max", String.valueOf(maxPower))
            );
            return;
        }
        final UUID target = context.getOfflinePlayerArgument("player").getUniqueId();
        record = this.dataService.getPlayerRecord(target);
        maxPower = this.playerService.getMaxPower(target);
        context.replyWith(
            this.constructMessage("CurrentPowerLevel")
                .with("power", String.valueOf(record.getPower()))
                .with("max", String.valueOf(maxPower))
                .with("name", context.getOfflinePlayerArgument("player").getName())
        );
    }
}