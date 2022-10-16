/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionJoinEvent;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.PlayerRecord;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.FactionService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TerritoryOwnerNotifier;
import dansplugins.factionsystem.utils.extended.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class JoinHandler implements Listener {
    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final ConfigService configService;
    private final Logger logger;
    private final Messenger messenger;
    private final TerritoryOwnerNotifier territoryOwnerNotifier;
    private final FactionService factionService;

    @Inject
    public JoinHandler(
        PersistentData persistentData,
        LocaleService localeService,
        ConfigService configService,
        Logger logger,
        Messenger messenger,
        TerritoryOwnerNotifier territoryOwnerNotifier,
        FactionService factionService
    ) {
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.configService = configService;
        this.logger = logger;
        this.messenger = messenger;
        this.territoryOwnerNotifier = territoryOwnerNotifier;
        this.factionService = factionService;
    }

    @EventHandler()
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (dataExistsForPlayer(player)) {
            PlayerRecord record = this.persistentData.getPlayerRecord(player.getUniqueId());
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
            player.sendMessage(ChatColor.GREEN + String.format(localeService.get("WelcomeBackLastLogout"), event.getPlayer().getName(), record.getTimeSinceLastLogout()));
        }

        if (record.getPowerLost() > 0) {
            player.sendMessage(ChatColor.RED + String.format(localeService.get("PowerHasDecayed"), record.getPowerLost(), newPower));
        }

        record.setPowerLost(0);
    }

    private double getNewPower(Player player) {
        PlayerRecord record = this.persistentData.getPlayerRecord(player.getUniqueId());

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
        PlayerRecord record = new PlayerRecord(player.getUniqueId(), 1);
        this.persistentData.addPlayerRecord(record);
    }

    private boolean dataExistsForPlayer(Player player) {
        return this.persistentData.hasPlayerRecord(player.getUniqueId());
    }

    private void assignPlayerToRandomFaction(Player player) {
        Faction faction = persistentData.getRandomFaction();
        if (faction != null) {
            FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
            Bukkit.getPluginManager().callEvent(joinEvent);
            if (joinEvent.isCancelled()) {
                logger.debug("Join event was cancelled.");
                return;
            }
            messenger.sendAllPlayersInFactionMessage(faction, String.format(ChatColor.GREEN + "" + localeService.get("HasJoined"), player.getName(), faction.getName()));
            faction.addMember(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "" + localeService.get("AssignedToRandomFaction"));

            logger.debug(player.getName() + " has been randomly assigned to " + faction.getName() + "!");
        } else {
            logger.debug("Attempted to assign " + player.getName() + " to a random faction, but no factions are existent.");
        }
    }

    private void setPlayerActionBarTerritoryInfo(Player player) {
        if (configService.getBoolean("territoryIndicatorActionbar")) {
            if (chunkIsClaimed(player)) {
                String factionName = persistentData.getChunkDataAccessor().getClaimedChunk(player.getLocation().getChunk()).getHolder();
                Faction holder = persistentData.getFaction(factionName);
                territoryOwnerNotifier.sendPlayerTerritoryAlert(player, holder);
                return;
            }

            territoryOwnerNotifier.sendPlayerTerritoryAlert(player, null);
        }
    }

    private boolean chunkIsClaimed(Player player) {
        return persistentData.getChunkDataAccessor().isClaimed(player.getLocation().getChunk());
    }

    private void informPlayerIfTheirFactionIsWeakened(Player player) {
        Faction playersFaction = persistentData.getPlayersFaction(player.getUniqueId());
        if (playersFaction == null) {
            return;
        }

        if (playersFaction.isLiege() && this.factionService.isWeakened(playersFaction)) {
            player.sendMessage(ChatColor.RED + localeService.get("AlertFactionIsWeakened"));
        }
    }
}