/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.time.ZonedDateTime;
import java.util.UUID;
import com.google.gson.annotations.Expose;

import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jdbi.v3.core.mapper.Nested;
/**
 * @author Daniel McCoy Stephenson
 */
public class PlayerRecord {
    @Expose
    @ColumnName("id")
    private UUID uuid;
    @Expose
    @Nested
    private PlayerStats stats;
    @Expose
    @ColumnName("power")
    private double powerLevel = 0;

    public PlayerRecord() { }

    public PlayerRecord(UUID uuid, int initialLogins, double initialPowerLevel) {
        this.uuid = uuid;
        this.stats = new PlayerStats(initialLogins);
        this.powerLevel = initialPowerLevel;
    }

    public UUID getPlayerUUID() {
        return this.uuid;
    }

    public double getPower() {
        return this.powerLevel;
    }

    public void setPower(double newPower) {
        this.powerLevel = newPower;
    }


    // Convenience methods
    public int getLogins() {
        return this.stats.getLogins();
    }

    public String getActiveSessionLength() {
        return this.stats.getActiveSessionLength();
    }

    public int getMinutesSinceLastLogout() {
        return this.stats.getMinutesSinceLastLogout();
    }

    public ZonedDateTime getLastLogout() {
        return this.stats.getLastLogout();
    }

    public double getPowerLost() {
        return this.stats.getPowerLost();
    }

    public void setPowerLost(int power) {
        this.stats.setPowerLost(power);
    }

    public void setLastLogout(ZonedDateTime dateTime) {
        this.stats.setLastLogout(dateTime);
    }

    public void incrementLogins() {
        this.stats.incrementLogins();
    }

    public String getTimeSinceLastLogout() {
        return this.stats.getTimeSinceLastLogout();
    }

    public void increasePowerLostBy(double amount) {
        this.stats.increasePowerLostBy(amount);
    }
}