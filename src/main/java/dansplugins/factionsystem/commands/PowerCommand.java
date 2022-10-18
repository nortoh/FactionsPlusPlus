/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.PlayerRecord;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class PowerCommand extends Command {

    private final PersistentData persistentData;
    private final PlayerService playerService;

    @Inject
    public PowerCommand(
        PersistentData persistentData,
        PlayerService playerService
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
                )
        );
        this.persistentData = persistentData;
        this.playerService = playerService;
    }

    public void execute(CommandContext context) {
        final PlayerRecord record;
        double maxPower;
        if (context.getRawArguments().length == 0) {
            if (context.isConsole()) {
                context.replyWith("OnlyPlayersCanUseCommand");
                return;
            }
            record = this.persistentData.getPlayerRecord(context.getPlayer().getUniqueId());
            maxPower = this.playerService.getMaxPower(context.getPlayer().getUniqueId());
            context.replyWith(
                this.constructMessage("AlertCurrentPowerLevel")
                    .with("power", String.valueOf(record.getPower()))
                    .with("max", String.valueOf(maxPower))
            );
            return;
        }
        final UUID target = context.getOfflinePlayerArgument("player").getUniqueId();
        record = this.persistentData.getPlayerRecord(target);
        maxPower = this.playerService.getMaxPower(target);
        context.replyWith(
            this.constructMessage("CurrentPowerLevel")
                .with("power", String.valueOf(record.getPower()))
                .with("max", String.valueOf(maxPower))
                .with("name", context.getOfflinePlayerArgument("player").getName())
        );
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return TabCompleteTools.allOnlinePlayersMatching(args[0]);
    }
}