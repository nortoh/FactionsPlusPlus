package factionsplusplus.events.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;

import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

@Singleton
public class SpawnHandler implements Listener {
    private final ConfigService configService;
    private final DataService dataService;

    @Inject
    public SpawnHandler(ConfigService configService, DataService dataService) {
        this.configService = configService;
        this.dataService = dataService;
    }

    @EventHandler()
    public void handle(EntitySpawnEvent event) {
        if (this.isLandClaimed(event) && event.getEntity() instanceof Monster && ! this.configService.getBoolean("mobsSpawnInFactionTerritory")) {
            event.setCancelled(true);
        }
    }

    private boolean isLandClaimed(EntitySpawnEvent event) {
        return this.dataService.isChunkClaimed(event.getLocation().getChunk());
    }
}