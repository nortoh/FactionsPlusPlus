/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.events.FactionWarStartEvent;
import factionsplusplus.factories.WarFactory;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.FactionService;
import factionsplusplus.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class DeclareWarCommand extends Command {

    private final ConfigService configService;
    private final PersistentData persistentData;
    private final WarFactory warFactory;
    private final FactionRepository factionRepository;
    private final FactionService factionService;

    @Inject
    public DeclareWarCommand(
        ConfigService configService,
        PersistentData persistentData,
        WarFactory warFactory,
        FactionRepository factionRepository,
        FactionService factionService
    ) {
        super(
            new CommandBuilder()
                .withName("grantindependence")
                .withAliases("dw", LOCALE_PREFIX + "CmdDeclareWar")
                .withDescription("Declare war on a faction.")
                .requiresPermissions("mf.declarewar")
                .expectsPlayerExecution()
                .expectsNoFactionMembership()
                .expectsFactionOfficership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to declare war on")
                        .expectsFaction()
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
        this.persistentData = persistentData;
        this.warFactory = warFactory;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
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

            if (!faction.getLiege().equals(opponent.getLiege())) {
                final Faction enemyLiege = this.factionRepository.getByID(opponent.getLiege());
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
        if (!warStartEvent.isCancelled()) {
            // Make enemies.
            faction.addEnemy(opponent.getID());
            opponent.addEnemy(faction.getID());
            warFactory.createWar(faction, opponent, context.getStringArgument("reason"));
        }
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            final List<String> factionsAllowedtoWar = new ArrayList<>();
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            ArrayList<UUID> playerEnemies = playerFaction.getEnemyFactions();
            ArrayList<UUID> playerAllies = playerFaction.getAllies();
            for(Faction faction : this.persistentData.getFactions()) {
                // If the faction is not an ally and they are not already enemied to them
                if(!playerAllies.contains(faction.getID()) && !playerEnemies.contains(faction.getID()) && !faction.getID().equals(playerFaction.getID())) {
                    factionsAllowedtoWar.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWithAddQuotes(args[0], factionsAllowedtoWar);
        }
        return null;
    }
}