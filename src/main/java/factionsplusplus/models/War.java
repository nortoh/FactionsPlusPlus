/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import factionsplusplus.jsonadapters.ZonedDateTimeAdapter;


import java.time.ZonedDateTime;
import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 */
public class War {
    @Expose
    @ColumnName("attacker_id")
    private UUID attacker;
    @Expose
    @ColumnName("defender_id")
    private UUID defender;
    @Expose
    private String reason;
    @Expose
    @JsonAdapter(ZonedDateTimeAdapter.class)
    @ColumnName("started_at")
    private ZonedDateTime started;
    @Expose
    @JsonAdapter(ZonedDateTimeAdapter.class)
    @ColumnName("ended_at")
    private ZonedDateTime ended;
    @Expose
    @ColumnName("is_active")
    private boolean active;

    public War() { }
    
    public War(Faction attacker, Faction defender, String reason) {
        this.attacker = attacker.getID();
        this.defender = defender.getID();
        this.reason = reason;
        this.started = ZonedDateTime.now();
        this.active = true;
    }

    public UUID getAttacker() {
        return this.attacker;
    }

    public UUID getDefender() {
        return this.defender;
    }

    public String getReason() {
        return this.reason;
    }

    public ZonedDateTime getStartDate() {
        return this.started;
    }

    public ZonedDateTime getEndDate() {
        return this.ended;
    }

    public boolean isActive() {
        return this.active;
    }

    public void end() {
        this.active = false;
        this.ended = ZonedDateTime.now();
    }
}