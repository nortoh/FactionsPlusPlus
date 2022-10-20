package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.integrators.DynmapIntegrator;

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
}