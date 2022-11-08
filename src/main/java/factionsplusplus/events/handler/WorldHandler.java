package factionsplusplus.events.handler;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.services.DataService;

@Singleton
public class WorldHandler implements Listener {

    private final DataService dataService;

    @Inject
    public WorldHandler(DataService dataService) {
        this.dataService = dataService;
    }

    @EventHandler()
    public void handle(WorldLoadEvent event) {
        this.createWorldIfMissing(event.getWorld());
    }

    @EventHandler()
    public void handle(WorldInitEvent event) {
        this.createWorldIfMissing(event.getWorld());
    }

    // Create a world if it doesn't already exist.
    private void createWorldIfMissing(World world) {
        if (this.dataService.getWorld(world.getUID()) == null) {
            this.dataService.getWorldRepository().create(world);
        }
    }
    
}
