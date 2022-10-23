package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.events.FactionWarEndEvent;
import factionsplusplus.events.FactionWarStartEvent;
import factionsplusplus.services.DynmapIntegrationService;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
public class WarHandler implements Listener {
    private final DynmapIntegrationService dynmapIntegrationService;

    @Inject
    public WarHandler(DynmapIntegrationService dynmapIntegrationService) {
        this.dynmapIntegrationService = dynmapIntegrationService;
    }

    @EventHandler()
    public void handle(FactionWarStartEvent event) {
        this.dynmapIntegrationService.changeFactionsVisibility(List.of(event.getAttacker(), event.getDefender()), false);
    }

    @EventHandler()
    public void handle(FactionWarEndEvent event) {
        this.dynmapIntegrationService.changeFactionsVisibility(List.of(event.getAttacker(), event.getDefender()), true);
    }
}