/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.services.ClaimService;
import factionsplusplus.services.DataService;
import factionsplusplus.services.DynmapIntegrationService;
import factionsplusplus.services.FactionService;
import factionsplusplus.services.MessageService;
import factionsplusplus.utils.TerritoryOwnerNotifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;
import java.util.UUID;
import static org.bukkit.Bukkit.getServer;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class MoveHandler implements Listener {
    private final TerritoryOwnerNotifier territoryOwnerNotifier;
    private final FactionsPlusPlus factionsPlusPlus;
    private final DynmapIntegrationService dynmapService;
    private final FactionService factionService;
    private final MessageService messageService;
    private final DataService dataService;
    private final ClaimService claimService;

    @Inject
    public MoveHandler(
        TerritoryOwnerNotifier territoryOwnerNotifier,
        FactionsPlusPlus factionsPlusPlus,
        DynmapIntegrationService dynmapService,
        FactionService factionService,
        MessageService messageService,
        DataService dataService,
        ClaimService claimService
    ) {
        this.territoryOwnerNotifier = territoryOwnerNotifier;
        this.factionsPlusPlus = factionsPlusPlus;
        this.dynmapService = dynmapService;
        this.factionService = factionService;
        this.messageService = messageService;
        this.dataService = dataService;
        this.claimService = claimService;
    }

    @EventHandler()
    public void handle(PlayerMoveEvent event) {
        if (this.playerEnteredANewChunk(event)) {
            Player player = event.getPlayer();

            this.initiateAutoclaimCheck(player);

            if (this.newChunkIsClaimedAndOldChunkWasNot(event)) {
                UUID factionUUID = this.dataService.getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
                Faction holder = this.dataService.getFaction(factionUUID);
                this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, holder);
                return;
            }

            if (this.newChunkIsUnclaimedAndOldChunkWasNot(event)) {
                this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, null);
                return;
            }

            if (this.newChunkIsClaimedAndOldChunkWasAlsoClaimed(event) && this.chunkHoldersAreNotEqual(event)) {
                UUID factionUUID = this.dataService.getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
                Faction holder = this.dataService.getFaction(factionUUID);
                this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, holder);
            }

        }
    }

    /**
     * This event handler method will deal with liquid moving from one block to another.
     */
    @EventHandler()
    public void handle(BlockFromToEvent event) {
        ClaimedChunk fromChunk = this.dataService.getClaimedChunk(event.getBlock().getChunk());
        ClaimedChunk toChunk = this.dataService.getClaimedChunk(event.getToBlock().getChunk());

        if (this.playerMovedFromUnclaimedLandIntoClaimedLand(fromChunk, toChunk)) {
            event.setCancelled(true);
            return;
        }

        if (this.playerMovedFromClaimedLandIntoClaimedLand(fromChunk, toChunk) && this.holdersOfChunksAreDifferent(fromChunk, toChunk)) {
            event.setCancelled(true);
        }
    }

    private boolean playerEnteredANewChunk(PlayerMoveEvent event) {
        return event.getFrom().getChunk() != Objects.requireNonNull(event.getTo()).getChunk();
    }

    private void initiateAutoclaimCheck(Player player) {
        Faction playersFaction = this.dataService.getPlayersFaction(player.getUniqueId());
        if (playersFaction != null && playersFaction.isOwner(player.getUniqueId())) {
            if (playersFaction.getAutoClaimStatus()) {
                if (this.notAtDemesneLimit(playersFaction)) {
                    this.scheduleClaiming(player, playersFaction);
                } else {
                    this.messageService.sendLocalizedMessage(player, "AlertReachedDemesne");
                }
            }
        }
    }

    private boolean notAtDemesneLimit(Faction faction) {
        return this.dataService.getClaimedChunksForFaction(faction).size() < this.factionService.getCumulativePowerLevel(faction);
    }

    private void scheduleClaiming(Player player, Faction faction) {
        getServer().getScheduler().runTaskLater(factionsPlusPlus, () -> {
            // add new chunk to claimed chunks
            this.claimService.claimChunkAtLocation(player, player.getLocation(), faction);
            this.dynmapService.updateClaimsIfAble();
        }, 1); // delayed by 1 tick (1/20th of a second) because otherwise players will claim the chunk they just left
    }

    private boolean newChunkIsClaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return this.dataService.isChunkClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && !this.dataService.isChunkClaimed(event.getFrom().getChunk());
    }

    private boolean newChunkIsUnclaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return !this.dataService.isChunkClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && this.dataService.isChunkClaimed(event.getFrom().getChunk());
    }

    private boolean newChunkIsClaimedAndOldChunkWasAlsoClaimed(PlayerMoveEvent event) {
        return this.dataService.isChunkClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && this.dataService.isChunkClaimed(event.getFrom().getChunk());
    }

    private boolean chunkHoldersAreNotEqual(PlayerMoveEvent event) {
        return !(this.dataService.getClaimedChunk(event.getFrom().getChunk()).getHolder().equals(this.dataService.getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder()));
    }

    private boolean playerMovedFromUnclaimedLandIntoClaimedLand(ClaimedChunk fromChunk, ClaimedChunk toChunk) {
        return fromChunk == null && toChunk != null;
    }

    private boolean holdersOfChunksAreDifferent(ClaimedChunk fromChunk, ClaimedChunk toChunk) {
        return !fromChunk.getHolder().equals(toChunk.getHolder());
    }

    private boolean playerMovedFromClaimedLandIntoClaimedLand(ClaimedChunk fromChunk, ClaimedChunk toChunk) {
        return fromChunk != null && toChunk != null;
    }

}