/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.objects.domain.Duel;
import dansplugins.factionsystem.objects.domain.Duel.DuelState;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.DeathService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DuelCommand extends Command {
    private final EphemeralData ephemeralData;
    private final MessageService messageService;
    private final MedievalFactions medievalFactions;
    private final DeathService deathService;

    @Inject
    public DuelCommand(
        EphemeralData ephemeralData,
        MessageService messageService,
        MedievalFactions medievalFactions,
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
        this.medievalFactions = medievalFactions;
        this.deathService = deathService;
    }


    public void cancelCommand(CommandContext context) {
        Player player = context.getPlayer();
        if(!this.isDuelling(player)) {
            context.replyWith("AlertNoPendingChallenges");
            return;
        }
        final Duel duel = this.getDuel(player);
        if (duel == null) {
            context.replyWith("AlertNoPendingChallenges");
            return;
        }
        if (duel.getStatus().equals(DuelState.DUELLING)) {
            context.replyWith("CannotCancelActiveDuel");
            return;
        }
        this.ephemeralData.getDuelingPlayers().remove(duel);
        context.replyWith("DuelChallengeCancelled");
    }

    public void acceptCommand(CommandContext context) {
        Player player = context.getPlayer();
        if (this.isDuelling(player)) {
            context.replyWith("AlertAlreadyDuelingSomeone");
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
            context.replyWith(
                this.constructMessage("AlertNotBeenChallengedByPlayer")
                    .with("name", target.getName())
            );
            return;
        }
        if (duel.getStatus().equals(DuelState.DUELLING)) {
            context.replyWith(
                this.constructMessage("AlertAlreadyDuelingPlayer")
                    .with("name", target.getName())
            );
            return;
        }
        if (!duel.isChallenged(player)) {
            context.replyWith(
                this.constructMessage("AlertNotBeenChallengedByPlayer")
                    .with("name", target.getName())
            );
        }
        duel.acceptDuel();
    }

    public void challengeCommand(CommandContext context) {
        Player target = context.getPlayerArgument("player");
        Player player = context.getPlayer();
        if (target == player) {
            context.replyWith("CannotDuelSelf");
            return;
        }
        if (this.isDuelling(player)) {
            context.replyWith("AlertAlreadyDuelingSomeone");
            return;
        }
        if (this.isDuelling(target)) {
            context.replyWith(
                this.constructMessage("PlayerAlreadyDueling")
                    .with("name", target.getName())
            );
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
        this.ephemeralData.getDuelingPlayers().add(new Duel(this.medievalFactions, this.ephemeralData, this.deathService, player, target, limit));
    }

    /**
     * Method to handle tab completion.
     *
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return TabCompleteTools.completeMultipleOptions(args[0], "challenge", "accept", "cancel");
        } else if (args.length == 2) {
            if (args[0] == "challenge") return TabCompleteTools.allOnlinePlayersMatching(args[1]);
        }
        return null;
    }
}