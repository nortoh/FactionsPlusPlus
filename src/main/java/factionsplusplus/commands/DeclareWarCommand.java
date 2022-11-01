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
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.data.repositories.FactionRepository;
import factionsplusplus.data.repositories.WarRepository;
import factionsplusplus.events.internal.FactionWarStartEvent;
import factionsplusplus.builders.ArgumentBuilder;


/**
 * @author Callum Johnson
 */
@Singleton
public class DeclareWarCommand extends Command {

    private final ConfigService configService;
    private final FactionRepository factionRepository;
    private final FactionService factionService;
    private final WarRepository warRepository;

    @Inject
    public DeclareWarCommand(
        ConfigService configService,
        FactionRepository factionRepository,
        FactionService factionService,
        WarRepository warRepository
    ) {
        super(
            new CommandBuilder()
                .withName("declarewar")
                .withAliases("dw", LOCALE_PREFIX + "CmdDeclareWar")
                .withDescription("Declare war on a faction.")
                .requiresPermissions("mf.declarewar")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOfficership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to declare war on")
                        .expectsFaction()
                        .addFilters(ArgumentFilterType.NotAllied, ArgumentFilterType.NotEnemy, ArgumentFilterType.NotOwnFaction)
                        .expectsDoubleQuotes()
                        .isRequired()
                )
                .addArgument(
                    "reason",
                    new ArgumentBuilder()
                        .setDescription("the reason for declaring war")
                        .expectsString()
                        .expectsDoubleQuotes()
                        .isOptional()
                )
        );
        this.configService = configService;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
        this.warRepository = warRepository;
    }

    public void execute(CommandContext context) {
        final Player player = context.getPlayer();
        final Faction faction = context.getExecutorsFaction();
        final Faction opponent = context.getFactionArgument("faction name");

        if (opponent == faction) {
            context.replyWith("CannotDeclareWarOnYourself");
            return;
        }

        if (faction.isEnemy(opponent.getID())) {
            context.replyWith(
                this.constructMessage("AlertAlreadyAtWarWith")
                    .with("faction", opponent.getName())
            );
            return;
        }

        if (faction.hasLiege() && opponent.hasLiege()) {
            if (faction.isVassal(opponent.getID())) {
                context.replyWith("CannotDeclareWarOnVassal");
                return;
            }

            if (! faction.getLiege().equals(opponent.getLiege())) {
                final Faction enemyLiege = this.factionRepository.get(opponent.getLiege());
                if (this.factionService.calculateCumulativePowerLevelWithoutVassalContribution(enemyLiege) <
                        this.factionService.getMaximumCumulativePowerLevel(enemyLiege) / 2) {
                    context.replyWith("CannotDeclareWarIfLiegeNotWeakened");
                }
            }
        }

        if (faction.isLiege(opponent.getID())) {
            context.replyWith("CannotDeclareWarOnLiege");
            return;
        }

        if (faction.isAlly(opponent.getID())) {
            context.replyWith("CannotDeclareWarOnAlly");
            return;
        }

        if (this.configService.getBoolean("allowNeutrality") && (opponent.getFlag("neutral").toBoolean())) {
            context.replyWith("CannotDeclareWarOnNeutralFaction");
            return;
        }

        if (this.configService.getBoolean("allowNeutrality") && (faction.getFlag("neutral").toBoolean())) {
            context.replyWith("CannotDeclareWarIfNeutralFaction");
            return;
        }

        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(faction, opponent, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (! warStartEvent.isCancelled()) {
            // Make enemies.
            Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    faction.upsertRelation(opponent.getID(), FactionRelationType.Enemy);
                    String reason = context.getStringArgument("reason");
                    if (reason == null) reason = "No reason";
                    warRepository.create(faction, opponent, reason);
                }
            });
        }
    }
}