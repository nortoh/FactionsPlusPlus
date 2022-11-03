package factionsplusplus.events.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import factionsplusplus.services.DynmapIntegrationService;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import factionsplusplus.events.internal.FactionWarEndEvent;
import factionsplusplus.events.internal.FactionWarStartEvent;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
public class WarHandler implements Listener {
    private final DynmapIntegrationService dynmapIntegrationService;
    private final BukkitAudiences adventure;

    @Inject
    public WarHandler(DynmapIntegrationService dynmapIntegrationService, @Named("adventure") BukkitAudiences adventure) {
        this.dynmapIntegrationService = dynmapIntegrationService;
        this.adventure = adventure;
    }

    @EventHandler()
    public void handle(FactionWarStartEvent event) {
        this.adventure.players().sendMessage(
            Component.translatable("GlobalNotice.War.Declared").color(NamedTextColor.YELLOW).args(Component.text(event.getAttacker().getName()), Component.text(event.getDefender().getName())),
            MessageType.SYSTEM
        );
        this.dynmapIntegrationService.changeFactionsVisibility(List.of(event.getAttacker(), event.getDefender()), false);
    }

    @EventHandler()
    public void handle(FactionWarEndEvent event) {
        this.dynmapIntegrationService.changeFactionsVisibility(List.of(event.getAttacker(), event.getDefender()), true);
    }
}