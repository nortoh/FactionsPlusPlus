/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.PersistentData;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.services.ConfigService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import static org.bukkit.Material.LADDER;

import javax.inject.Provider;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class InteractionAccessChecker {
    private final Provider<PersistentData> persistentData;
    private final ConfigService configService;
    private final EphemeralData ephemeralData;
    private final Logger logger;

    @Inject
    public InteractionAccessChecker(Provider<PersistentData> persistentData, ConfigService configService, EphemeralData ephemeralData, Logger logger) {
        this.persistentData = persistentData;
        this.configService = configService;
        this.ephemeralData = ephemeralData;
        this.logger = logger;
    }

    public boolean shouldEventBeCancelled(ClaimedChunk claimedChunk, Player player) {
        if (factionsProtectionsNotEnabled()) {
            return false;
        }

        if (claimedChunk == null) {
            return false;
        }

        if (isPlayerBypassing(player)) {
            return false;
        }

        Faction playersFaction = this.persistentData.get().getPlayersFaction(player.getUniqueId());
        if (playersFaction == null) {
            return true;
        }

        return !isLandClaimedByPlayersFaction(playersFaction, claimedChunk) && !isOutsiderInteractionAllowed(player, claimedChunk, playersFaction);
    }

    private boolean isLandClaimedByPlayersFaction(Faction faction, ClaimedChunk claimedChunk) {
        return faction.getID().equals(claimedChunk.getHolder());
    }

    private boolean factionsProtectionsNotEnabled() {
        return !configService.getBoolean("factionProtectionsEnabled");
    }

    private boolean isPlayerBypassing(Player player) {
        return ephemeralData.getAdminsBypassingProtections().contains(player.getUniqueId());
    }

    public boolean isOutsiderInteractionAllowed(Player player, ClaimedChunk chunk, Faction playersFaction) {
        if (!configService.getBoolean("factionProtectionsEnabled")) {
            return true;
        }

        final Faction chunkHolder = this.persistentData.get().getFactionByID(chunk.getHolder());

        boolean inVassalageTree = this.persistentData.get().isPlayerInFactionInVassalageTree(player, chunkHolder);
        boolean isAlly = playersFaction.isAlly(chunk.getHolder());
        boolean allyInteractionAllowed = chunkHolder.getFlag("alliesCanInteractWithLand").toBoolean();
        boolean vassalageTreeInteractionAllowed = chunkHolder.getFlag("vassalageTreeCanInteractWithLand").toBoolean();

        logger.debug("allyInteractionAllowed: " + allyInteractionAllowed);
        logger.debug("vassalageTreeInteractionAllowed: " + vassalageTreeInteractionAllowed);

        boolean allowed = allyInteractionAllowed && isAlly;

        if (vassalageTreeInteractionAllowed && inVassalageTree) {
            allowed = true;
        }

        return allowed;
    }

    public boolean isPlayerAttemptingToPlaceLadderInEnemyTerritoryAndIsThisAllowed(Block blockPlaced, Player player, ClaimedChunk claimedChunk) {
        Faction playersFaction = this.persistentData.get().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            return false;
        }

        if (claimedChunk == null) {
            return false;
        }

        boolean laddersArePlaceableInEnemyTerritory = configService.getBoolean("laddersPlaceableInEnemyFactionTerritory");
        boolean playerIsTryingToPlaceLadderInEnemyTerritory = blockPlaced.getType() == LADDER && playersFaction.isEnemy(claimedChunk.getHolder());
        return laddersArePlaceableInEnemyTerritory && playerIsTryingToPlaceLadderInEnemyTerritory;
    }
}