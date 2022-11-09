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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.data.repositories.WarRepository;
import factionsplusplus.events.internal.FactionWarStartEvent;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class DeclareWarCommand extends Command {

    private final ConfigService configService;
    private final WarRepository warRepository;

    @Inject
    public DeclareWarCommand(
        ConfigService configService,
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
        this.warRepository = warRepository;
    }

    public void execute(CommandContext context) {
        final Player player = context.getPlayer();
        final Faction faction = context.getExecutorsFaction();
        final Faction opponent = context.getFactionArgument("faction name");

        if (opponent == faction) {
            context.error("Error.War.Self");
            return;
        }

        if (faction.isEnemy(opponent.getUUID())) {
            context.error("Error.War.AlreadyAtWar", faction.getName(), opponent.getName());
            return;
        }

        if (faction.hasLiege() && opponent.hasLiege()) {
            if (faction.isVassal(opponent.getUUID())) {
                context.error("Error.War.Vassal");
                return;
            }

            if (! faction.getLiege().equals(opponent.getLiege())) {
                final Faction enemyLiege = opponent.getLiege();
                if (enemyLiege.calculateCumulativePowerLevelWithoutVassalContribution() <
                        enemyLiege.getMaximumCumulativePowerLevel() / 2) {
                    context.error("Error.War.LiegeNotWeakened");
                }
            }
        }

        if (faction.isLiege(opponent.getUUID())) {
            context.error("Error.War.Liege");
            return;
        }

        if (faction.isAlly(opponent.getUUID())) {
            context.error("Error.War.Ally");
            return;
        }

        if (this.configService.getBoolean("faction.allowNeutrality") && (opponent.getFlag("neutral").toBoolean())) {
            context.error("Error.War.Neutral.Target");
            return;
        }

        if (this.configService.getBoolean("faction.allowNeutrality") && (faction.getFlag("neutral").toBoolean())) {
            context.error("Error.War.Neutral.Source");
            return;
        }

        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(faction, opponent, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (! warStartEvent.isCancelled()) {
            // Make enemies.
            Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    faction.upsertRelation(opponent.getUUID(), FactionRelationType.Enemy);
                    String reason = context.getStringArgument("reason");
                    if (reason == null) reason = "No reason";
                    warRepository.create(faction, opponent, reason);
                }
            });
        }
    }
}