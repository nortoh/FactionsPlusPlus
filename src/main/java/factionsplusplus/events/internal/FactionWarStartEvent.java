/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.events.internal;

import factionsplusplus.events.internal.abs.FactionEvent;
import factionsplusplus.models.Faction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * @author DanTheTechMan#3438
 */
public class FactionWarStartEvent extends FactionEvent implements Cancellable {

    private final Faction attacker;
    private final Faction defender;
    private final Player declarer;
    // Variables.
    private boolean cancelled;

    /**
     * Constructor to initialise a FactionWarStartEvent.
     * <p>
     * This event is called when a faction declares war on another faction.
     * </p>
     *
     * @param attacker - Faction declaring war.
     * @param defender - Faction getting declared war on.
     * @param declarer - Player responsible.
     */
    public FactionWarStartEvent(Faction attacker, Faction defender, Player declarer) {
        super(attacker, declarer);
        this.attacker = attacker;
        this.defender = defender;
        this.declarer = declarer;
        this.cancelled = false;
    }

    // Cancellable methodology.
    @Override
    public boolean isCancelled() {
        return this.cancelled;
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

    /**
     * Returns the Player object for who declared war.
     *
     * @return the player that declared a war.
     * @see Player
     */
    public Player getDeclarer() {
        return this.declarer;
    }
}