/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.events;

import factionsplusplus.events.abs.FactionEvent;
import factionsplusplus.models.Faction;
import org.bukkit.event.Cancellable;

/**
 * @author DanTheTechMan#3438
 */
public class FactionWarEndEvent extends FactionEvent implements Cancellable {

    private final Faction attacker;
    private final Faction defender;
    // Variables.
    private boolean cancelled = false;

    /**
     * Constructor to initialise a FactionWarEndEvent.
     * <p>
     * This event is called when a war ends due to a peace agreement.
     * </p>
     *
     * @param attacker - First faction involved.
     * @param defender - Second faction involved.
     */
    public FactionWarEndEvent(Faction attacker, Faction defender) {
        super(attacker);
        this.attacker = attacker;
        this.defender = defender;
    }

    // Cancellable methodology.
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Returns the Faction object that started the attack.
     *
     * @return the faction that started a war
     * @see Faction
     */
    public Faction getAttacker() {
        return this.attacker;
    }

    /**
     * Returns the Faction object that is defending
     *
     * @return the faction that is defending the war
     * @see Faction
     */
    public Faction getDefender() {
        return this.defender;
    }
}