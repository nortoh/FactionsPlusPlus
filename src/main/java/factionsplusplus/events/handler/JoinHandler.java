/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.events.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.models.FPPPlayer;
import factionsplusplus.services.ClaimService;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.services.DynmapIntegrationService;
import factionsplusplus.utils.Logger;
import factionsplusplus.utils.TerritoryOwnerNotifier;
import net.kyori.adventure.text.format.NamedTextColor;
import factionsplusplus.data.factories.PlayerFactory;
import factionsplusplus.events.internal.FactionJoinEvent;

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
    private final DataService dataService;
    private final ClaimService claimService;
    private final DynmapIntegrationService dynmapIntegrationService;
    private final PlayerFactory playerFactory;

    @Inject
    public JoinHandler(
        ConfigService configService,
        Logger logger,
        TerritoryOwnerNotifier territoryOwnerNotifier,
        DataService dataService,
        ClaimService claimService,
        DynmapIntegrationService dynmapIntegrationService,
        PlayerFactory playerFactory
    ) {
        this.configService = configService;
        this.logger = logger;
        this.territoryOwnerNotifier = territoryOwnerNotifier;
        this.dataService = dataService;
        this.claimService = claimService;
        this.dynmapIntegrationService = dynmapIntegrationService;
        this.playerFactory = playerFactory;
    }

    @EventHandler()
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.dataExistsForPlayer(player)) {
            FPPPlayer record = this.dataService.getPlayer(player.getUniqueId());
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
        Faction playerFaction = this.dataService.getPlayersFaction(uuid);

        if (playerFaction != null && ! playerFaction.getEnemies().isEmpty()) {
            this.dynmapIntegrationService.changePlayersVisibility(List.of(uuid), false);
            return;
        }

        this.dynmapIntegrationService.changePlayersVisibility(List.of(uuid), true);
    }

    private void handlePowerDecay(FPPPlayer record, Player player, PlayerJoinEvent event) {
        double newPower = getNewPower(player);

        if (record.getLastLogout() != null && record.getMinutesSinceLastLogout() > 1) {
            record.alert("PlayerNotice.WelcomeBack", NamedTextColor.GREEN, event.getPlayer().getName(), record.getTimeSinceLastLogout());
        }

        if (record.getPowerLost() > 0) {
            record.alert("PlayerNotice.PowerDecayed", NamedTextColor.YELLOW, record.getPowerLost(), newPower);
        }

        record.setPowerLost(0);
    }

    private double getNewPower(Player player) {
        FPPPlayer record = this.dataService.getPlayer(player.getUniqueId());

        double newPower = record.getPower();
        if (newPower < 0) {
            return 0;
        }
        return newPower;
    }

    private void handleRandomFactionAssignmentIfNecessary(Player player) {
        if (this.configService.getBoolean("player.assignNewPlayersToRandomFaction")) {
            this.assignPlayerToRandomFaction(player);
        }
    }

    private void createRecordsForPlayer(Player player) {
        FPPPlayer record = this.playerFactory.create(player.getUniqueId(), 1, this.configService.getDouble("player.power.initial"));
        this.dataService.getPlayerRepository().create(record);
    }

    private boolean dataExistsForPlayer(Player player) {
        return this.dataService.hasPlayer(player);
    }

    private void assignPlayerToRandomFaction(Player player) {
        Faction faction = this.dataService.getRandomFaction();
        if (faction != null) {
            FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
            Bukkit.getPluginManager().callEvent(joinEvent);
            if (joinEvent.isCancelled()) {
                this.logger.debug("Join event was cancelled.");
                return;
            }
            faction.alert("FactionNotice.PlayerJoined", player.getName());
            faction.addMember(player.getUniqueId());
            FPPPlayer member = this.dataService.getPlayer(player.getUniqueId());
            member.alert("PlayerNotice.RandomFactionAssignment", faction.getName());
            this.logger.debug(player.getName() + " has been randomly assigned to " + faction.getName() + "!");
        } else {
            this.logger.debug("Attempted to assign " + player.getName() + " to a random faction, but no factions are existent.");
        }
    }

    private void setPlayerActionBarTerritoryInfo(Player player) {
        if (this.configService.getBoolean("faction.indicator.actionbar")) {
            ClaimedChunk chunk = this.dataService.getClaimedChunk(player.getLocation().getChunk());
            if (chunk != null) {
                this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, this.dataService.getFaction(chunk.getHolder()));
                return;
            }

            this.territoryOwnerNotifier.sendPlayerTerritoryAlert(player, null);
        }
    }

    private void informPlayerIfTheirFactionIsWeakened(Player player) {
        Faction playersFaction = this.dataService.getPlayersFaction(player.getUniqueId());
        if (playersFaction == null) {
            return;
        }

        if (playersFaction.isLiege() && playersFaction.isWeakened()) {
            FPPPlayer member = this.dataService.getPlayer(player.getUniqueId());
            member.alert("FactionNotice.Weakened", NamedTextColor.RED);
        }
    }
}