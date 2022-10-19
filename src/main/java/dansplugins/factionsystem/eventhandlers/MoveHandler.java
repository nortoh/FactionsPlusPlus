/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.ClaimedChunk;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.DynmapIntegrationService;
import dansplugins.factionsystem.services.FactionService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.TerritoryOwnerNotifier;
import org.bukkit.ChatColor;
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
    private final PersistentData persistentData;
    private final TerritoryOwnerNotifier territoryOwnerNotifier;
    private final MedievalFactions medievalFactions;
    private final DynmapIntegrationService dynmapService;
    private final FactionService factionService;
    private final MessageService messageService;

    @Inject
    public MoveHandler(
        PersistentData persistentData,
        TerritoryOwnerNotifier territoryOwnerNotifier,
        MedievalFactions medievalFactions,
        DynmapIntegrationService dynmapService,
        FactionService factionService,
        MessageService messageService
    ) {
        this.persistentData = persistentData;
        this.territoryOwnerNotifier = territoryOwnerNotifier;
        this.medievalFactions = medievalFactions;
        this.dynmapService = dynmapService;
        this.factionService = factionService;
        this.messageService = messageService;
    }

    @EventHandler()
    public void handle(PlayerMoveEvent event) {
        if (this.playerEnteredANewChunk(event)) {
            Player player = event.getPlayer();

            this.initiateAutoclaimCheck(player);

            if (this.newChunkIsClaimedAndOldChunkWasNot(event)) {
                UUID factionUUID = this.persistentData.getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
                Faction holder = this.persistentData.getFactionByID(factionUUID);
                this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, holder);
                return;
            }

            if (this.newChunkIsUnclaimedAndOldChunkWasNot(event)) {
                this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, null);
                return;
            }

            if (this.newChunkIsClaimedAndOldChunkWasAlsoClaimed(event) && this.chunkHoldersAreNotEqual(event)) {
                UUID factionUUID = this.persistentData.getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
                Faction holder = this.persistentData.getFactionByID(factionUUID);
                this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, holder);
            }

        }
    }

    /**
     * This event handler method will deal with liquid moving from one block to another.
     */
    @EventHandler()
    public void handle(BlockFromToEvent event) {
        ClaimedChunk fromChunk = this.persistentData.getChunkDataAccessor().getClaimedChunk(event.getBlock().getChunk());
        ClaimedChunk toChunk = this.persistentData.getChunkDataAccessor().getClaimedChunk(event.getToBlock().getChunk());

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
        Faction playersFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
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
        return persistentData.getChunkDataAccessor().getChunksClaimedByFaction(faction.getID()) < this.factionService.getCumulativePowerLevel(faction);
    }

    private void scheduleClaiming(Player player, Faction faction) {
        getServer().getScheduler().runTaskLater(medievalFactions, () -> {
            // add new chunk to claimed chunks
            this.persistentData.getChunkDataAccessor().claimChunkAtLocation(player, player.getLocation(), faction);
            this.dynmapService.updateClaimsIfAble();
        }, 1); // delayed by 1 tick (1/20th of a second) because otherwise players will claim the chunk they just left
    }

    private boolean newChunkIsClaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return this.persistentData.getChunkDataAccessor().isClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && !this.persistentData.getChunkDataAccessor().isClaimed(event.getFrom().getChunk());
    }

    private boolean newChunkIsUnclaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return !this.persistentData.getChunkDataAccessor().isClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && this.persistentData.getChunkDataAccessor().isClaimed(event.getFrom().getChunk());
    }

    private boolean newChunkIsClaimedAndOldChunkWasAlsoClaimed(PlayerMoveEvent event) {
        return this.persistentData.getChunkDataAccessor().isClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && this.persistentData.getChunkDataAccessor().isClaimed(event.getFrom().getChunk());
    }

    private boolean chunkHoldersAreNotEqual(PlayerMoveEvent event) {
        return !(this.persistentData.getChunkDataAccessor().getClaimedChunk(event.getFrom().getChunk()).getHolder().equals(this.persistentData.getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder()));
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