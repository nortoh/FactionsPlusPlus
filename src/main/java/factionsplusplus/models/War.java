/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import factionsplusplus.beans.WarBean;
import factionsplusplus.models.interfaces.Identifiable;
import factionsplusplus.repositories.WarRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * @author Daniel McCoy Stephenson
 */
public class War implements Identifiable {
    @ColumnName("id")
    private UUID uuid;
    @ColumnName("attacker_id")
    private UUID attacker;
    @ColumnName("defender_id")
    private UUID defender;
    private String reason;
    @ColumnName("started_at")
    private ZonedDateTime started;
    @ColumnName("ended_at")
    private ZonedDateTime ended;
    @ColumnName("is_active")
    private boolean active;

    private final WarRepository warRepository;

    @AssistedInject
    public War(WarRepository warRepository) {
        this.warRepository = warRepository;
     }
    
    @AssistedInject
    public War(@Assisted("attacker") Faction attacker, @Assisted("defender") Faction defender, @Assisted String reason, WarRepository warRepository) {
        this.uuid = UUID.randomUUID();
        this.attacker = attacker.getID();
        this.defender = defender.getID();
        this.reason = reason;
        this.started = ZonedDateTime.now();
        this.active = true;
        this.warRepository = warRepository;
    }

    @AssistedInject
    public War(@Assisted WarBean bean, WarRepository warRepository) {
        this.uuid = bean.getId();
        this.attacker = bean.getAttacker();
        this.defender = bean.getDefender();
        this.reason = bean.getReason();
        this.started = bean.getStartedAt();
        this.ended = bean.getEndedAt();
        this.active = bean.isActive();
        this.warRepository = warRepository;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getAttacker() {
        return this.attacker;
    }
    
    public void setAttacker(UUID uuid) {
        this.attacker = uuid;
    }

    public UUID getDefender() {
        return this.defender;
    }

    public void setDefender(UUID uuid) {
        this.defender = uuid;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ZonedDateTime getStartDate() {
        return this.started;
    }

    public void setStartDate(ZonedDateTime dateTime) {
        this.started = dateTime;
    }

    public ZonedDateTime getEndDate() {
        return this.ended;
    }

    public void setEndDate(ZonedDateTime dateTime) {
        this.ended = dateTime;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean value) {
        this.active = value;
    }

    public void end() {
        this.active = false;
        this.ended = ZonedDateTime.now();
        this.persist();
    }

    public void persist() {
        this.warRepository.persist(this);
    }
}