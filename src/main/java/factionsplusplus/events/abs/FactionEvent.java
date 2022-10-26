/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.events.abs;

import factionsplusplus.models.Faction;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author C A L L U M#4160
 */
public abstract class FactionEvent extends Event {

    // Constants.
    private static final HandlerList handlers = new HandlerList();

    // Variables.
    private final Faction faction;
    private OfflinePlayer offlinePlayer = null;

    /**
     * Constructor for a FactionEvent with a reference to a Faction.
     *
     * @param faction related to the event.
     */
    public FactionEvent(Faction faction) {
        this.faction = faction;
    }

    /**
     * Constructor for a FactionEvent with a reference to both a Faction and player.
     *
     * @param faction related to the event.
     * @param player  related to the event.
     */
    public FactionEvent(Faction faction, OfflinePlayer player) {
        this.faction = faction;
        this.offlinePlayer = player;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    // Getters.
    public Faction getFaction() {
        return this.faction;
    }

    public OfflinePlayer getOfflinePlayer() {
        return this.offlinePlayer;
    }

    // Bukkit Event API requirements.
    public @NotNull HandlerList getHandlers() {
        return FactionEvent.handlers;
    }

}
