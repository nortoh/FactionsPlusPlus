package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.ActionBarService;
import factionsplusplus.services.DataService;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.ZonedDateTime;

@Singleton
public class QuitHandler implements Listener {
    private final EphemeralData ephemeralData;
    private final ActionBarService actionBarService;
    private final DataService dataService;

    @Inject
    public QuitHandler(EphemeralData ephemeralData, DataService dataService, ActionBarService actionBarService) {
        this.ephemeralData = ephemeralData;
        this.dataService = dataService;
        this.actionBarService = actionBarService;
    }

    @EventHandler()
    public void handle(PlayerQuitEvent event) {
        ephemeralData.getLockingPlayers().remove(event.getPlayer().getUniqueId());
        ephemeralData.getUnlockingPlayers().remove(event.getPlayer().getUniqueId());
        ephemeralData.getPlayersGrantingAccess().remove(event.getPlayer().getUniqueId());
        ephemeralData.getPlayersCheckingAccess().remove(event.getPlayer().getUniqueId());
        ephemeralData.getPlayersRevokingAccess().remove(event.getPlayer().getUniqueId());

        PlayerRecord record = this.dataService.getPlayerRecord(event.getPlayer().getUniqueId());
        if (record != null) {
            record.setLastLogout(ZonedDateTime.now());
        }

        actionBarService.clearPlayerActionBar(event.getPlayer());
    }
}