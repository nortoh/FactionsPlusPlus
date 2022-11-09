/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import static org.bukkit.Material.LADDER;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class InteractionAccessChecker {
    private final ConfigService configService;
    private final Logger logger;
    private final DataService dataService;

    @Inject
    public InteractionAccessChecker(ConfigService configService, Logger logger, DataService dataService) {
        this.configService = configService;
        this.logger = logger;
        this.dataService = dataService;
    }

    public boolean shouldEventBeCancelled(ClaimedChunk claimedChunk, Player player) {
        // if faction protections are disabled, don't cancel
        if (this.factionsProtectionsNotEnabled()) {
            return false;
        }

        // if claimed chunk is null (doesn't exist), don't cancel
        if (claimedChunk == null) {
            return false;
        }

        // if player is admin bypassing, don't cancel
        if (this.isPlayerBypassing(player)) {
            return false;
        }

        // if player is not part of a faction, return
        Faction playersFaction = this.dataService.getPlayersFaction(player.getUniqueId());
        if (playersFaction == null) {
            return true;
        }

        return ! this.isLandClaimedByPlayersFaction(playersFaction, claimedChunk) && ! this.isOutsiderInteractionAllowed(player, claimedChunk, playersFaction);
    }

    private boolean isLandClaimedByPlayersFaction(Faction faction, ClaimedChunk claimedChunk) {
        return faction.getUUID().equals(claimedChunk.getHolder());
    }

    private boolean factionsProtectionsNotEnabled() {
        return ! this.configService.getBoolean("faction.protections.interactions.enabled");
    }

    private boolean isPlayerBypassing(Player player) {
        return this.dataService.getPlayer(player.getUniqueId()).isAdminBypassing();
    }

    public boolean isOutsiderInteractionAllowed(Player player, ClaimedChunk chunk, Faction playersFaction) {
        if (! this.configService.getBoolean("faction.protections.interactions.enabled")) {
            return true;
        }

        final Faction chunkHolder = this.dataService.getFaction(chunk.getHolder());

        boolean inVassalageTree = this.dataService.isPlayerInVassalageTree(player, chunkHolder);
        boolean isAlly = playersFaction.isAlly(chunk.getHolder());
        boolean allyInteractionAllowed = chunkHolder.getFlag("alliesCanInteractWithLand").toBoolean();
        boolean vassalageTreeInteractionAllowed = chunkHolder.getFlag("vassalageTreeCanInteractWithLand").toBoolean();

        this.logger.debug("allyInteractionAllowed: " + allyInteractionAllowed);
        this.logger.debug("vassalageTreeInteractionAllowed: " + vassalageTreeInteractionAllowed);

        boolean allowed = allyInteractionAllowed && isAlly;

        if (vassalageTreeInteractionAllowed && inVassalageTree) {
            allowed = true;
        }

        return allowed;
    }

    public boolean isPlayerAttemptingToPlaceLadderInEnemyTerritoryAndIsThisAllowed(Block blockPlaced, Player player, ClaimedChunk claimedChunk) {
        Faction playersFaction = this.dataService.getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            return false;
        }

        if (claimedChunk == null) {
            return false;
        }

        boolean laddersArePlaceableInEnemyTerritory = this.configService.getBoolean("faction.protections.laddersPlaceableByEnemies");
        boolean playerIsTryingToPlaceLadderInEnemyTerritory = blockPlaced.getType() == LADDER && playersFaction.isEnemy(claimedChunk.getHolder());
        return laddersArePlaceableInEnemyTerritory && playerIsTryingToPlaceLadderInEnemyTerritory;
    }
}