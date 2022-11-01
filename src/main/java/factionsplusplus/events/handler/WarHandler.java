package factionsplusplus.events.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.services.DynmapIntegrationService;
import factionsplusplus.services.MessageService;

import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.events.internal.FactionWarEndEvent;
import factionsplusplus.events.internal.FactionWarStartEvent;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
public class WarHandler implements Listener {
    private final DynmapIntegrationService dynmapIntegrationService;
    private final MessageService messageService;

    @Inject
    public WarHandler(DynmapIntegrationService dynmapIntegrationService, MessageService messageService) {
        this.dynmapIntegrationService = dynmapIntegrationService;
        this.messageService = messageService;
    }

    @EventHandler()
    public void handle(FactionWarStartEvent event) {
        this.messageService.sendAllPlayersLocalizedMessage(
            new MessageBuilder("HasDeclaredWarAgainst")
                .with("f_a", event.getAttacker().getName())
                .with("f_b", event.getDefender().getName())
        );
        this.dynmapIntegrationService.changeFactionsVisibility(List.of(event.getAttacker(), event.getDefender()), false);
    }

    @EventHandler()
    public void handle(FactionWarEndEvent event) {
        this.dynmapIntegrationService.changeFactionsVisibility(List.of(event.getAttacker(), event.getDefender()), true);
    }
}