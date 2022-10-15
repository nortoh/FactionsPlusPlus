package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.integrators.DynmapIntegrator;

import org.bukkit.Bukkit;

@Singleton
public class DynmapIntegrationService {
    private final DynmapIntegrator dynmapIntegrator;

    @Inject
    public DynmapIntegrationService(MedievalFactions plugin) {
        if (Bukkit.getPluginManager().getPlugin("dynmap") != null) {
            this.dynmapIntegrator = plugin.getInjector().getInstance(DynmapIntegrator.class);
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
        if (this.dynmapIntegrator != null) this.dynmapIntegrator.updateClaims();
    }
}
