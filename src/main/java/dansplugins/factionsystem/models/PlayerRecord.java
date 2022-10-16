/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.models;

import java.time.ZonedDateTime;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

import com.google.gson.JsonElement;

/**
 * @author Daniel McCoy Stephenson
 */
public class PlayerRecord {
    @Expose
    private UUID playerUUID;
    @Expose
    private PlayerStats stats;
    @Expose
    private double powerLevel = 0;

    public PlayerRecord(UUID playerUUID, int initialLogins) {
        this.playerUUID = playerUUID;
        this.stats = new PlayerStats(initialLogins);
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public double getPower() {
        return powerLevel;
    }

    public void setPower(double newPower) {
        powerLevel = newPower;
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

    public int getPowerLost() {
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

    // reimplement in PlayerService
    public double maxPower() {
        return 0;
        /*
        if (isPlayerAFactionOwner(playerUUID)) {
            return (int) (configService.getDouble("initialMaxPowerLevel") * configService.getDouble("factionOwnerMultiplier"));
        }

        if (isPlayerAFactionOfficer(playerUUID)) {
            return (int) (configService.getDouble("initialMaxPowerLevel") * configService.getDouble("factionOfficerMultiplier"));
        }

        return configService.getInt("initialMaxPowerLevel");
        */
    }
    public void decreasePower() {
        /*
        if (powerLevel > 0) {
            powerLevel -= configService.getInt("powerDecreaseAmount");
            if (powerLevel < 0) {
                powerLevel = 0;
            }
        }
        */
    }
    public void increasePower() {
        /*
        if (powerLevel < maxPower()) {
            powerLevel += configService.getInt("powerIncreaseAmount");
            if (powerLevel > maxPower()) {
                powerLevel = maxPower();
            }
        }
        */
    }

    public double revokePowerDueToDeath() {
        return 0;
        /*
        double powerLost = configService.getDouble("powerLostOnDeath");
        powerLevel = Math.max(powerLevel - powerLost, 0);
        return powerLost;
        */
    }

    public void grantPowerDueToKill() {
        /*
        double powerGained = configService.getDouble("powerGainedOnKill");
        powerLevel = Math.min(powerLevel + powerGained, maxPower());
        */
    }

    public void incrementPowerLost() {
        this.stats.incrementPowerLost();
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