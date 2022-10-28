package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.ActionBarService;
import factionsplusplus.services.DataService;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.ZonedDateTime;

@Singleton
public class QuitHandler implements Listener {
    private final EphemeralData ephemeralData;
    private final ActionBarService actionBarService;
    private final DataService dataService;
    private final FactionsPlusPlus plugin;

    @Inject
    public QuitHandler(EphemeralData ephemeralData, DataService dataService, ActionBarService actionBarService, FactionsPlusPlus plugin) {
        this.ephemeralData = ephemeralData;
        this.dataService = dataService;
        this.actionBarService = actionBarService;
        this.plugin = plugin;
    }

    @EventHandler()
    public void handle(PlayerQuitEvent event) {
        this.ephemeralData.getPlayersPendingInteraction().remove(event.getPlayer().getUniqueId());

        final PlayerRecord record = this.dataService.getPlayerRecord(event.getPlayer().getUniqueId());
        if (record != null) {
            record.setLastLogout(ZonedDateTime.now());
        }

        this.actionBarService.clearPlayerActionBar(event.getPlayer());

        // Save player to database on quit
        if (record != null) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                @Override
                public void run() {
                    dataService.getPlayerRecordRepository().persist(record);
                }
            });
        }
    }
}