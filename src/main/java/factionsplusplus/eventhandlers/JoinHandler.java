/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.events.FactionJoinEvent;
import factionsplusplus.models.Faction;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.services.FactionService;
import factionsplusplus.services.MessageService;
import factionsplusplus.utils.Logger;
import factionsplusplus.utils.TerritoryOwnerNotifier;
import factionsplusplus.builders.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class JoinHandler implements Listener {
    private final PersistentData persistentData;
    private final ConfigService configService;
    private final Logger logger;
    private final TerritoryOwnerNotifier territoryOwnerNotifier;
    private final FactionService factionService;
    private final DataService dataService;
    private final MessageService messageService;

    @Inject
    public JoinHandler(
        PersistentData persistentData,
        ConfigService configService,
        Logger logger,
        TerritoryOwnerNotifier territoryOwnerNotifier,
        FactionService factionService,
        DataService dataService,
        MessageService messageService
    ) {
        this.persistentData = persistentData;
        this.configService = configService;
        this.logger = logger;
        this.territoryOwnerNotifier = territoryOwnerNotifier;
        this.factionService = factionService;
        this.dataService = dataService;
        this.messageService = messageService;
    }

    @EventHandler()
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (dataExistsForPlayer(player)) {
            PlayerRecord record = this.dataService.getPlayerRecord(player.getUniqueId());
            record.incrementLogins();
            handlePowerDecay(record, player, event);
        } else {
            createRecordsForPlayer(player);
            handleRandomFactionAssignmentIfNecessary(player);
        }
        setPlayerActionBarTerritoryInfo(event.getPlayer());
        persistentData.getChunkDataAccessor().informPlayerIfTheirLandIsInDanger(player);
        informPlayerIfTheirFactionIsWeakened(player);
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
        if (configService.getBoolean("randomFactionAssignment")) {
            assignPlayerToRandomFaction(player);
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
            logger.debug(player.getName() + " has been randomly assigned to " + faction.getName() + "!");
        } else {
            logger.debug("Attempted to assign " + player.getName() + " to a random faction, but no factions are existent.");
        }
    }

    private void setPlayerActionBarTerritoryInfo(Player player) {
        if (configService.getBoolean("territoryIndicatorActionbar")) {
            if (chunkIsClaimed(player)) {
                UUID factionUUID = dataService.getClaimedChunk(player.getLocation().getChunk()).getHolder();
                Faction holder = dataService.getFaction(factionUUID);
                territoryOwnerNotifier.sendPlayerTerritoryAlert(player, holder);
                return;
            }

            territoryOwnerNotifier.sendPlayerTerritoryAlert(player, null);
        }
    }

    private boolean chunkIsClaimed(Player player) {
        return this.dataService.isChunkClaimed(player.getLocation().getChunk());
    }

    private void informPlayerIfTheirFactionIsWeakened(Player player) {
        Faction playersFaction = dataService.getPlayersFaction(player.getUniqueId());
        if (playersFaction == null) {
            return;
        }

        if (playersFaction.isLiege() && this.factionService.isWeakened(playersFaction)) {
            this.messageService.sendLocalizedMessage(player, "AlertFactionIsWeakened");
        }
    }
}