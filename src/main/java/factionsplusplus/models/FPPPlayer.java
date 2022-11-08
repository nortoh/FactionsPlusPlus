/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

import factionsplusplus.constants.GroupRole;
import factionsplusplus.data.beans.PlayerBean;
import factionsplusplus.models.interfaces.Identifiable;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.utils.StringUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
/**
 * @author Daniel McCoy Stephenson
 */
public class FPPPlayer implements Identifiable, ForwardingAudience.Single {
    private UUID uuid;
    private double powerLevel;
    private boolean adminBypass = false;
    private int logins = 0;
    private double powerLost = 0;
    private ZonedDateTime lastLogout = ZonedDateTime.now();

    private final BukkitAudiences adventure;
    private final DataService dataService;
    private final ConfigService configService;

    @AssistedInject
    public FPPPlayer(
        @Named("adventure") BukkitAudiences adventure,
        DataService dataService,
        ConfigService configService
    ) { 
        this.adventure = adventure;
        this.dataService = dataService;
        this.configService = configService;
    }

    @AssistedInject
    public FPPPlayer(
        @Assisted UUID uuid,
        @Assisted int initialLogins,
        @Assisted double initialPowerLevel,
        @Named("adventure") BukkitAudiences adventure,
        DataService dataService,
        ConfigService configService
    ) {
        this.uuid = uuid;
        this.logins = initialLogins;
        this.powerLevel = initialPowerLevel;
        this.adventure = adventure;
        this.dataService = dataService;
        this.configService = configService;
    }

    @AssistedInject
    public FPPPlayer(
        @Assisted PlayerBean bean,
        @Named("adventure") BukkitAudiences adventure,
        DataService dataService,
        ConfigService configService
    ) {
        this.uuid = bean.getId();
        this.powerLevel = bean.getPower();
        this.adminBypass = bean.isAdminBypassing();
        this.powerLost = bean.getOfflinePowerLost();
        this.lastLogout = bean.getLastLogout();
        this.logins = bean.getLoginCount();
        this.adventure = adventure;
        this.dataService = dataService;
        this.configService = configService;
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

    // Tools
    public double increasePowerBy(double increaseAmount) {
        if (this.getPower() < this.getMaxPower()) {
            this.setPower(Math.min((this.getPower() + increaseAmount), this.getMaxPower()));
        }
        return this.getPower();
    }

    public double decreasePowerBy(double decreaseAmount) {
        if (this.getPower() > 0) {
            this.setPower(Math.max((this.getPower() - decreaseAmount), 0));
        }
        this.increasePowerLostBy(decreaseAmount);
        return this.getPower();
    }

    // Calculations
    public double getMaxPower() {
        double initialPowerLevel = this.configService.getDouble("initialMaxPowerLevel");
        switch(GroupRole.getFromLevel(this.dataService.getPlayersFaction(this.uuid).getMember(uuid).getRole())) {
            case Owner:
                return (double)(initialPowerLevel * this.configService.getDouble("factionOwnerMultiplier"));
            case Officer:
                return (double)(initialPowerLevel * this.configService.getDouble("factionOfficerMultiplier"));
            default:
                return initialPowerLevel;
        }
    }

    // Get as bukkit OfflinePlayer
    public OfflinePlayer toBukkitOfflinePlayer() {
        return Bukkit.getOfflinePlayer(this.uuid);
    }

    // Get as bukkit Player
    public Player toBukkitPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    @Override
    public Audience audience() {
        return this.toBukkitPlayer() == null ? Audience.empty() : this.adventure.player(this.toBukkitPlayer());
    }

    public void message(ComponentLike message, MessageType type) {
        this.audience().sendMessage(message, type);
    }

    public void alert(ComponentLike message) {
        this.message(message, MessageType.SYSTEM);
    }

    public void alert(String localizationKey, TextColor color, Object... arguments) {
        this.alert(
            Component.translatable(localizationKey).color(color).args(Arrays.stream(arguments).map(argument -> Component.text(argument.toString())).toList())
        );
    }

    public void alert(String localizationKey, Object... arguments) {
        this.alert(localizationKey, NamedTextColor.YELLOW, arguments);
    }

    public void success(String localizationKey, Object... arguments) {
        this.alert(
            Component.text()
                .append(
                    Component.translatable("Generic.Success").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                )
                .append(Component.text(" "))
                .append(
                    Component.translatable(localizationKey).color(NamedTextColor.AQUA).args(Arrays.stream(arguments).map(argument -> Component.text(argument.toString())).toList())
                )
                .asComponent()
        );
    }

    public void error(String localizationKey, Object... arguments) {
        this.alert(
            Component.text()
                .append(
                    Component.translatable("Generic.Error").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                )
                .append(Component.text(" "))
                .append(
                    Component.translatable(localizationKey).color(NamedTextColor.YELLOW).args(Arrays.stream(arguments).map(argument -> Component.text(argument.toString())).toList())
                )
                .asComponent()
        );
    }

}