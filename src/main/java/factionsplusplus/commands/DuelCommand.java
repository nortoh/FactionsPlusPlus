/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Duel;
import factionsplusplus.models.Duel.DuelState;
import factionsplusplus.services.MessageService;
import factionsplusplus.services.DeathService;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class DuelCommand extends Command {
    private final EphemeralData ephemeralData;
    private final MessageService messageService;
    private final FactionsPlusPlus factionsPlusPlus;
    private final DeathService deathService;

    @Inject
    public DuelCommand(
        EphemeralData ephemeralData,
        MessageService messageService,
        FactionsPlusPlus factionsPlusPlus,
        DeathService deathService
    ) {
        super(
            new CommandBuilder()
                .withName("duel")
                .withAliases("dl", LOCALE_PREFIX + "CmdDuel")
                .withDescription("Duel other players.")
                .requiresPermissions("mf.duel")
                .requiresSubCommand()
                .expectsPlayerExecution()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("challenge")
                        .withAliases(LOCALE_PREFIX + "CmdDuelChallenge")
                        .withDescription("Challenge a player to a duel.")
                        .setExecutorMethod("challengeCommand")
                        .addArgument(
                            "player",
                            new ArgumentBuilder()
                                .setDescription("the player to challenge")
                                .expectsOnlinePlayer()
                                .addFilters(ArgumentFilterType.ExcludeSelf)
                                .isRequired()
                        )
                        .addArgument(
                            "time limit",
                            new ArgumentBuilder()
                                .setDescription("the time limit for the player to acccept, in seconds")
                                .expectsInteger()
                                .isOptional()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("accept")
                        .withAliases(LOCALE_PREFIX + "CmdDuelAccept")
                        .withDescription("Accept a duel request.")
                        .setExecutorMethod("acceptCommand")
                        .addArgument(
                            "player",
                            new ArgumentBuilder()
                                .setDescription("the player whose duel you wish to acccept")
                                .expectsOnlinePlayer()
                                .isOptional()
                        )
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("cancel")
                        .withAliases(LOCALE_PREFIX + "CmdDuelCancel")
                        .withDescription("Cancel a pending duel.")
                        .setExecutorMethod("cancelCommand")
                )
        );
        this.ephemeralData = ephemeralData;
        this.messageService = messageService;
        this.factionsPlusPlus = factionsPlusPlus;
        this.deathService = deathService;
    }


    public void cancelCommand(CommandContext context) {
        Player player = context.getPlayer();
        if (! this.isDuelling(player)) {
            context.error("Error.Duel.NoSentChallenges");
            return;
        }
        final Duel duel = this.getDuel(player);
        if (duel == null) {
            context.error("Error.Duel.NoSentChallenges");
            return;
        }
        if (duel.getStatus().equals(DuelState.DUELLING)) {
            context.error("Error.Duel.CannotCancelActive");
            return;
        }
        this.ephemeralData.getDuelingPlayers().remove(duel);
        context.replyWith("DuelChallengeCancelled");
    }

    public void acceptCommand(CommandContext context) {
        Player player = context.getPlayer();
        if (this.isDuelling(player)) {
            context.error("Error.Duel.InDuel");
            return;
        }
        Player target = context.getPlayerArgument("player");
        final Duel duel;
        if (target == null) {
            duel = this.getDuel(player);
            target = duel.getChallenger();
        } else {
            duel = this.ephemeralData.getDuel(player, target);
        }
        if (duel == null) {
            context.error("Error.Duel.NotChallengedBy", target.getName());
            return;
        }
        if (duel.getStatus().equals(DuelState.DUELLING)) {
            context.error("Error.Duel.AlreadyInDuelWith", target.getName());
            return;
        }
        if (! duel.isChallenged(player)) {
            context.error("Error.Duel.NotChallengedBy", target.getName());
        }
        duel.acceptDuel();
    }

    public void challengeCommand(CommandContext context) {
        Player target = context.getPlayerArgument("player");
        Player player = context.getPlayer();
        if (target == player) {
            context.error("Error.Duel.Self");
            return;
        }
        if (this.isDuelling(player)) {
            context.error("Error.Duel.InDuel");
            return;
        }
        if (this.isDuelling(target)) {
            context.error("Error.Duel.TargetAlreadyDueling", target.getName());
            return;
        }
        Integer timeLimit = context.getIntegerArgument("time limit");
        if (timeLimit == null) timeLimit = 120;
        timeLimit = Math.min(timeLimit, 120); // TODO: make maximum time configurable
        this.inviteDuel(player, target, timeLimit);
        context.replyWith(
            this.constructMessage("AlertChallengeIssued")
                .with("name", target.getName())
        );
    }

    private Duel getDuel(Player player) {
        return this.ephemeralData.getDuelingPlayers().stream().filter(duel -> duel.isChallenged(player) || duel.isChallenger(player)).findFirst().orElse(null);
    }

    private boolean isDuelling(Player player) {
        return this.ephemeralData.getDuelingPlayers().stream().anyMatch(duel -> duel.hasPlayer(player) && duel.getStatus().equals(DuelState.DUELLING));
    }

    private void inviteDuel(Player player, Player target, int limit) {
        this.messageService.sendLocalizedMessage(
            target,
            this.constructMessage("AlertChallengedToDuelPlusHowTo")
                .with("name", player.getName())
        );
        this.ephemeralData.getDuelingPlayers().add(new Duel(this.factionsPlusPlus, this.ephemeralData, this.deathService, player, target, limit));
    }
}