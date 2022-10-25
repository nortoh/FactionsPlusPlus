package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.integrators.DynmapIntegrator;
import factionsplusplus.models.Faction;
import factionsplusplus.models.GroupMember;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

@Singleton
public class DynmapIntegrationService {
    private DynmapIntegrator dynmapIntegrator = null;
    private final FactionsPlusPlus factionsPlusPlus;

    @Inject
    public DynmapIntegrationService(FactionsPlusPlus factionsPlusPlus) {
        this.factionsPlusPlus = factionsPlusPlus;
        this.checkForDynmap();
    }

    public void checkForDynmap() {
        if (Bukkit.getPluginManager().getPlugin("dynmap") != null) {
            this.dynmapIntegrator = this.factionsPlusPlus.getInjector().getInstance(DynmapIntegrator.class);
            this.factionsPlusPlus.getLogger().info(
                String.format(
                    "[%s] Hooked into Dynmap-API %s",
                    this.factionsPlusPlus.getName(),
                    this.dynmapIntegrator.getCoreVersion()
                )
            );
            this.dynmapIntegrator.scheduleClaimsUpdate(600);
            this.dynmapIntegrator.updateClaims();
        } else {
            this.dynmapIntegrator = null;
        }
    }

    public DynmapIntegrator getDynmapIntegrator() {
        return this.dynmapIntegrator;
    }

    public void updateClaimsIfAble() {
        if (this.dynmapIntegrator == null) this.checkForDynmap();
        if (this.dynmapIntegrator != null) this.dynmapIntegrator.updateClaims();
    }

    public void changeFactionsVisibility(List<Faction> factions, boolean visible) {
        if (this.dynmapIntegrator == null) this.checkForDynmap();
        if (this.dynmapIntegrator != null) {
            factions.stream()
            .forEach(faction -> this.changePlayersVisibility(new ArrayList<UUID>(faction.getMembers().keySet()), visible));
        }
    }

    public void changePlayersVisibility(List<UUID> players, boolean visible) {
        if (this.dynmapIntegrator == null) this.checkForDynmap();
        if (this.dynmapIntegrator != null) {
            players.stream().forEach(player -> this.dynmapIntegrator.changePlayerVisibility(player, visible));
        }
    }
}
