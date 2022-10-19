/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.events;

import factionsplusplus.events.abs.FactionEvent;
import factionsplusplus.models.Faction;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;

/**
 * @author C A L L U M#4160
 */
public class FactionDisbandEvent extends FactionEvent implements Cancellable {

    // Variables.
    private boolean cancelled = false;

    /**
     * Constructor to initialise a FactionDisbandEvent.
     * <p>
     * This event is called when a player disbands a Faction.
     * </p>
     *
     * @param faction disbanded
     * @param player  who disbanded the Faction.
     */
    public FactionDisbandEvent(Faction faction, OfflinePlayer player) {
        super(faction, player);
    }

    // Bukkit Cancellable methodology.
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

}
