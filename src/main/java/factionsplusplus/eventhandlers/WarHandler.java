package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.events.FactionWarEndEvent;
import factionsplusplus.events.FactionWarStartEvent;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DynmapIntegrationService;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

@Singleton
public class WarHandler implements Listener {
    private final ConfigService configService;
    private final PersistentData persistentData;
    private final DynmapIntegrationService dynmapIntegrationService;

    @Inject
    public WarHandler(ConfigService configService, PersistentData persistentData, DynmapIntegrationService dynmapIntegrationService) {
        this.configService = configService;
        this.persistentData = persistentData;
        this.dynmapIntegrationService = dynmapIntegrationService;
    }

    @EventHandler()
    public void handle(FactionWarStartEvent event) {
        this.dynmapIntegrationService.getDynmapIntegrator().changeFactionVisibility(event.getAttacker(), false);
        this.dynmapIntegrationService.getDynmapIntegrator().changeFactionVisibility(event.getDefender(), false);
    }

    @EventHandler()
    public void handle(FactionWarEndEvent event) {
        this.dynmapIntegrationService.getDynmapIntegrator().changeFactionVisibility(event.getAttacker(), true);
        this.dynmapIntegrationService.getDynmapIntegrator().changeFactionVisibility(event.getDefender(), true);
    }


    private boolean isLandClaimed(EntitySpawnEvent event) {
        return persistentData.getChunkDataAccessor().isClaimed(event.getLocation().getChunk());
    }
}