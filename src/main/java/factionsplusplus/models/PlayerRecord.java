/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import factionsplusplus.data.beans.PlayerBean;
import factionsplusplus.models.interfaces.Identifiable;
import factionsplusplus.services.MessageService;
import factionsplusplus.utils.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
/**
 * @author Daniel McCoy Stephenson
 */
public class PlayerRecord implements Identifiable {
    private UUID uuid;
    private double powerLevel;
    private boolean adminBypass = false;
    private int logins = 0;
    private double powerLost = 0;
    private ZonedDateTime lastLogout = ZonedDateTime.now();

    private final MessageService messageService;

    @AssistedInject
    public PlayerRecord(MessageService messageService) { 
        this.messageService = messageService;
    }

    @AssistedInject
    public PlayerRecord(@Assisted UUID uuid, @Assisted int initialLogins, @Assisted double initialPowerLevel, MessageService messageService) {
        this.uuid = uuid;
        this.logins = initialLogins;
        this.powerLevel = initialPowerLevel;
        this.messageService = messageService;
    }

    @AssistedInject
    public PlayerRecord(@Assisted PlayerBean bean, MessageService messageService) {
        this.uuid = bean.getId();
        this.powerLevel = bean.getPower();
        this.adminBypass = bean.isAdminBypassing();
        this.powerLost = bean.getOfflinePowerLost();
        this.lastLogout = bean.getLastLogout();
        this.logins = bean.getLoginCount();
        this.messageService = messageService;
    }

    public UUID getUUID() {
        return this.uuid;
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

    public boolean isAdminBypassing() {
        return this.adminBypass;
    }

    public void setAdminBypass(boolean value) {
        this.adminBypass = value;
    }

    public void toggleAdminBypassing() {
        this.adminBypass = ! this.adminBypass;
    }

    // Convenience methods
    public int getLogins() {
        return this.logins;
    }

    public ZonedDateTime getLastLogout() {
        return this.lastLogout;
    }

    public double getPowerLost() {
        return this.powerLost;
    }

    public void setPowerLost(int power) {
        this.powerLost = power;
    }

    public void setLastLogout(ZonedDateTime dateTime) {
        this.lastLogout = dateTime;
    }

    public void incrementLogins() {
        this.logins++;
    }

    public void increasePowerLostBy(double amount) {
        this.powerLost += amount;
    }

    public int getMinutesSinceLastLogout() {
        if (this.lastLogout == null) {
            return 0;
        }
        ZonedDateTime now = ZonedDateTime.now();
        Duration duration = Duration.between(this.lastLogout, now);
        double totalSeconds = duration.getSeconds();
        return (int) totalSeconds / 60;
    }

    /**
     * Method to obtain the current session length in dd:hh:mm:ss
     * <p>
     * If days are not found, hh:mm:ss are returned.
     * </p>
     *
     * @return formatted String dd:hh:mm:ss
     * @author Callum
     */
    public String getActiveSessionLength() {
        if (this.lastLogout == null) {
            return "00:00:00";
        }
        final ZonedDateTime now = ZonedDateTime.now();
        final Duration duration = Duration.between(this.lastLogout, now);
        long totalSeconds = duration.getSeconds();
        final long days = TimeUnit.SECONDS.toDays(totalSeconds);
        totalSeconds -= TimeUnit.DAYS.toSeconds(days); // Remove Days from Total.
        final long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        totalSeconds -= TimeUnit.HOURS.toSeconds(hours); // Remove Hours from Total.
        final long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds);
        totalSeconds -= TimeUnit.MINUTES.toSeconds(minutes); // Remove Minutes from Total.
        final long seconds = totalSeconds; // Last one is just the remainder.
        final String d = StringUtils.prefixWithZero(days), h = StringUtils.prefixWithZero(hours), m = StringUtils.prefixWithZero(minutes), s = StringUtils.prefixWithZero(seconds);
        return (d.equalsIgnoreCase("00") ? "" : d + ":") + h + ":" + m + ":" + s;
    }

    public String getTimeSinceLastLogout() {
        if (this.lastLogout != null) {
            ZonedDateTime now = ZonedDateTime.now();
            Duration duration = Duration.between(this.lastLogout, now);
            double totalSeconds = duration.getSeconds();
            int minutes = (int) totalSeconds / 60;
            int hours = minutes / 60;
            int days = hours / 24;
            int hoursSince = hours - (days * 24);
            return days + " days and " + hoursSince + " hours";
        } else {
            return null;
        }
    }

    // Get as bukkit OfflinePlayer
    public OfflinePlayer asBukkitOfflinePlayer() {
        return Bukkit.getOfflinePlayer(this.uuid);
    }

    // Get as bukkit Player
    public Player asBukkitPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    // Send a message to this player
    public void message(GenericMessageBuilder builder) {
        this.messageService.sendLocalizedMessage((CommandSender)this.asBukkitOfflinePlayer(), builder);
    }
}