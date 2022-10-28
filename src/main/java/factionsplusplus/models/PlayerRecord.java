/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.time.ZonedDateTime;
import java.util.UUID;
import com.google.gson.annotations.Expose;
import com.google.inject.assistedinject.AssistedInject;

import factionsplusplus.beans.PlayerBean;
import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import factionsplusplus.models.interfaces.Identifiable;
import factionsplusplus.services.MessageService;

import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jdbi.v3.core.mapper.Nested;
/**
 * @author Daniel McCoy Stephenson
 */
public class PlayerRecord implements Identifiable {
    @Expose
    @ColumnName("id")
    private UUID uuid;
    @Expose
    @Nested
    private PlayerStats stats;
    @Expose
    @ColumnName("power")
    private double powerLevel = 0;
    @ColumnName("is_admin_bypassing")
    private boolean adminBypass = false;

    private final MessageService messageService;

    @AssistedInject
    public PlayerRecord(MessageService messageService) { 
        this.messageService = messageService;
    }

    @AssistedInject
    public PlayerRecord(UUID uuid, int initialLogins, double initialPowerLevel, MessageService messageService) {
        this.uuid = uuid;
        this.stats = new PlayerStats(initialLogins);
        this.powerLevel = initialPowerLevel;
        this.messageService = messageService;
    }

    @AssistedInject
    public PlayerRecord(PlayerBean bean, MessageService messageService) {
        this.uuid = bean.getId();
        this.stats = bean.getStats();
        this.powerLevel = bean.getPower();
        this.adminBypass = bean.isAdminBypassing();
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