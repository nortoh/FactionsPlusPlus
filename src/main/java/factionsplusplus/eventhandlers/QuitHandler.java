package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.PersistentData;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.ActionBarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.ZonedDateTime;

@Singleton
public class QuitHandler implements Listener {
    private final EphemeralData ephemeralData;
    private final PersistentData persistentData;
    private final ActionBarService actionBarService;

    @Inject
    public QuitHandler(EphemeralData ephemeralData, PersistentData persistentData, ActionBarService actionBarService) {
        this.ephemeralData = ephemeralData;
        this.persistentData = persistentData;
        this.actionBarService = actionBarService;
    }

    @EventHandler()
    public void handle(PlayerQuitEvent event) {
        this.ephemeralData.getPlayersPendingInteraction().remove(event.getPlayer().getUniqueId());

        PlayerRecord record = this.persistentData.getPlayerRecord(event.getPlayer().getUniqueId());
        if (record != null) {
            record.setLastLogout(ZonedDateTime.now());
        }

        actionBarService.clearPlayerActionBar(event.getPlayer());
    }
}