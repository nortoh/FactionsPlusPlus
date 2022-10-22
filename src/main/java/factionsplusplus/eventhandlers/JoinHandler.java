/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.events.FactionJoinEvent;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.ClaimService;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.services.DynmapIntegrationService;
import factionsplusplus.services.FactionService;
import factionsplusplus.services.MessageService;
import factionsplusplus.utils.Logger;
import factionsplusplus.utils.TerritoryOwnerNotifier;
import factionsplusplus.builders.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class JoinHandler implements Listener {
    private final ConfigService configService;
    private final Logger logger;
    private final TerritoryOwnerNotifier territoryOwnerNotifier;
    private final FactionService factionService;
    private final DataService dataService;
    private final MessageService messageService;
    private final ClaimService claimService;
    private final DynmapIntegrationService dynmapIntegrationService;

    @Inject
    public JoinHandler(
        ConfigService configService,
        Logger logger,
        TerritoryOwnerNotifier territoryOwnerNotifier,
        FactionService factionService,
        DataService dataService,
        MessageService messageService,
        ClaimService claimService,
        DynmapIntegrationService dynmapIntegrationService
    ) {
        this.configService = configService;
        this.logger = logger;
        this.territoryOwnerNotifier = territoryOwnerNotifier;
        this.factionService = factionService;
        this.dataService = dataService;
        this.messageService = messageService;
        this.claimService = claimService;
        this.dynmapIntegrationService = dynmapIntegrationService;
    }

    @EventHandler()
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.dataExistsForPlayer(player)) {
            PlayerRecord record = this.dataService.getPlayerRecord(player.getUniqueId());
            record.incrementLogins();
            this.handlePowerDecay(record, player, event);
        } else {
            this.createRecordsForPlayer(player);
            this.handleRandomFactionAssignmentIfNecessary(player);
        }
        this.setPlayerActionBarTerritoryInfo(event.getPlayer());
        this.claimService.informPlayerIfTheirLandIsInDanger(player);
        this.informPlayerIfTheirFactionIsWeakened(player);
        this.handleDynmapMapVisibility(player.getUniqueId());
    }

    private void handleDynmapMapVisibility(UUID uuid) {
        Faction playerFaction = this.dataService.getFaction(uuid);

        // In the event a player is hidden, leaves faction & rejoins, we should reset their visibility
        if (playerFaction == null) {
            this.dynmapIntegrationService.changePlayersVisibility(List.of(uuid), true);
            return;
        }

        if (playerFaction.getEnemyFactions().size() > 0) {
            this.dynmapIntegrationService.changePlayersVisibility(List.of(uuid), false);
            return;
        }

        this.dynmapIntegrationService.changePlayersVisibility(List.of(uuid), true);
    }

    private void handlePowerDecay(PlayerRecord record, Player player, PlayerJoinEvent event) {
        double newPower = getNewPower(player);

        if (record.getLastLogout() != null && record.getMinutesSinceLastLogout() > 1) {
            this.messageService.sendLocalizedMessage(
                player,
                new MessageBuilder("WelcomeBackLastLogout")
                    .with("name", event.getPlayer().getName())
                    .with("duration", record.getTimeSinceLastLogout())
            );
        }

        if (record.getPowerLost() > 0) {
            this.messageService.sendLocalizedMessage(
                player,
                new MessageBuilder("PowerHasDecayed")
                    .with("loss", String.valueOf(record.getPowerLost()))
                    .with("power", String.valueOf(newPower))
            );
        }

        record.setPowerLost(0);
    }

    private double getNewPower(Player player) {
        PlayerRecord record = this.dataService.getPlayerRecord(player.getUniqueId());

        double newPower = record.getPower();
        if (newPower < 0) {
            return 0;
        }
        return newPower;
    }

    private void handleRandomFactionAssignmentIfNecessary(Player player) {
        if (this.configService.getBoolean("randomFactionAssignment")) {
            this.assignPlayerToRandomFaction(player);
        }
    }

    private void createRecordsForPlayer(Player player) {
        PlayerRecord record = new PlayerRecord(player.getUniqueId(), 1, this.configService.getInt("initialPowerLevel"));
        this.dataService.getPlayerRecordRepository().create(record);
    }

    private boolean dataExistsForPlayer(Player player) {
        return this.dataService.hasPlayerRecord(player);
    }

    private void assignPlayerToRandomFaction(Player player) {
        Faction faction = this.dataService.getRandomFaction();
        if (faction != null) {
            FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
            Bukkit.getPluginManager().callEvent(joinEvent);
            if (joinEvent.isCancelled()) {
                logger.debug("Join event was cancelled.");
                return;
            }
            this.messageService.sendFactionLocalizedMessage(
                faction,
                new MessageBuilder("HasJoined")
                    .with("name", player.getName())
                    .with("faction", faction.getName())
            );
            faction.addMember(player.getUniqueId());
            this.messageService.sendLocalizedMessage(player, "AssignedToRandomFaction");
            this.logger.debug(player.getName() + " has been randomly assigned to " + faction.getName() + "!");
        } else {
            this.logger.debug("Attempted to assign " + player.getName() + " to a random faction, but no factions are existent.");
        }
    }

    private void setPlayerActionBarTerritoryInfo(Player player) {
        if (this.configService.getBoolean("territoryIndicatorActionbar")) {
            ClaimedChunk chunk = this.dataService.getClaimedChunk(player.getLocation().getChunk());
            if (chunk != null) {
                this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, this.dataService.getFaction(chunk.getHolder()));
                return;
            }

            this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, null);
        }
    }

    private boolean chunkIsClaimed(Player player) {
        return this.dataService.isChunkClaimed(player.getLocation().getChunk());
    }

    private void informPlayerIfTheirFactionIsWeakened(Player player) {
        Faction playersFaction = this.dataService.getPlayersFaction(player.getUniqueId());
        if (playersFaction == null) {
            return;
        }

        if (playersFaction.isLiege() && this.factionService.isWeakened(playersFaction)) {
            this.messageService.sendLocalizedMessage(player, "AlertFactionIsWeakened");
        }
    }
}