/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.events;

import factionsplusplus.events.abs.FactionEvent;
import factionsplusplus.models.Faction;
import org.bukkit.event.Cancellable;

/**
 * @author C A L L U M#4160
 */
public class FactionRenameEvent extends FactionEvent implements Cancellable {

    // Variables.
    private final String current, proposed;
    private boolean cancelled = false;

    /**
     * Constructor to initialise a FactionRenameEvent.
     * <p>
     * This event is called when a Faction is renamed from 'x' to 'y'.
     * </p>
     *
     * @param faction      being renamed.
     * @param currentName  of the Faction.
     * @param proposedName or new name of the Faction.
     */
    public FactionRenameEvent(Faction faction, String currentName, String proposedName) {
        super(faction);
        this.current = currentName;
        this.proposed = proposedName;
    }

    // Getters.
    public String getCurrentName() {
        return this.current;
    }

    public String getProposedName() {
        return this.proposed;
    }

    // Bukkit Cancellable methodology.
    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
