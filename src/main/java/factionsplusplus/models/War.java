/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import factionsplusplus.models.Faction;
import factionsplusplus.jsonadapters.ZonedDateTimeAdapter;


import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 */
public class War {
    @Expose
    private UUID attacker;
    @Expose
    private UUID defender;
    @Expose
    private String reason;
    @Expose
    @JsonAdapter(ZonedDateTimeAdapter.class)
    private ZonedDateTime started;
    @Expose
    @JsonAdapter(ZonedDateTimeAdapter.class)
    private ZonedDateTime ended;
    @Expose
    private Boolean active;

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

    public Boolean isActive() {
        return this.active;
    }

    public void end() {
        this.active = false;
        this.ended = ZonedDateTime.now();
    }

    // Tools
    public JsonElement toJsonTree() {
        return new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .serializeNulls()
            .create()
            .toJsonTree(this);
    }
}