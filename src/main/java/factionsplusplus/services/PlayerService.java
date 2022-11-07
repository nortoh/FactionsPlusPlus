package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.factories.PlayerFactory;
import factionsplusplus.models.FPPPlayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.Arrays;
import java.util.Objects;

import static org.bukkit.Bukkit.getServer;


/**
 * Sends messages to players and the console.
 */
@Singleton
public class PlayerService {
    @Inject private ConfigService configService;
    @Inject private DataService dataService;
    @Inject private PlayerFactory playerFactory;

    public double increasePower(UUID playerUUID) {
        double increaseAmount = this.configService.getDouble("powerIncreaseAmount");
        this.dataService.getPlayer(playerUUID).increasePowerBy(increaseAmount);
        return increaseAmount;
    }

    public void decreasePower(UUID playerUUID) {
        this.dataService.getPlayer(playerUUID).decreasePowerBy(this.configService.getInt("powerDecreaseAmount"));
    }

    public void grantPowerDueToKill(UUID playerUUID) {
        this.dataService.getPlayer(playerUUID).increasePowerBy(this.configService.getDouble("powerGainedOnKill"));
    }

    public double revokePowerDueToDeath(UUID playerUUID) {
        double powerLost = this.configService.getDouble("powerLostOnDeath");
        this.dataService.getPlayer(playerUUID).decreasePowerBy(powerLost);
        return powerLost;
    }

    public void resetPowerLevels() {
        final double initialPowerLevel = this.configService.getDouble("initialPowerLevel");
        this.dataService.getPlayerRepository().all().values().forEach(record -> record.setPower(initialPowerLevel));
    }

    public void createActivityRecordForEveryOfflinePlayer() { // this method is to ensure that when updating to a version with power decay, even players who never log in again will experience power decay
        final double initialPowerLevel = this.configService.getDouble("initialPowerLevel");
        Arrays.stream(Bukkit.getOfflinePlayers())
            .filter(player -> this.dataService.getPlayer(player.getUniqueId()) == null)
            .forEach(player -> {
                FPPPlayer newRecord = this.playerFactory.create(player.getUniqueId(), 1, initialPowerLevel);
                newRecord.setLastLogout(ZonedDateTime.now());
                this.dataService.createPlayer(newRecord);
            });
    }

    public void initiatePowerIncreaseForAllPlayers() {
        Bukkit.getOnlinePlayers()
            .stream()
            .map(player -> this.dataService.getPlayer(player.getUniqueId()))
            .forEach(record -> this.initiatePowerIncrease(record));
    }

    private void initiatePowerIncrease(FPPPlayer record) {
        double maxPower = record.getMaxPower();
        if (record.getPower() < maxPower && Objects.requireNonNull(getServer().getPlayer(record.getPlayerUUID())).isOnline()) {
            record.alert("PlayerNotice.PowerIncreasedBy", this.increasePower(record.getPlayerUUID()));
        }
    }

    public void decreasePowerForInactivePlayers() {
        if (! this.configService.getBoolean("powerDecreases")) return;
        final int minutesBeforePowerDecrease = this.configService.getInt("minutesBeforePowerDecrease");
        this.dataService.getPlayers()
            .stream()
            .filter(record -> {
                Player player = getServer().getPlayer(record.getPlayerUUID());
                if (player == null || ! player.isOnline()) {
                    if (record.getMinutesSinceLastLogout() > minutesBeforePowerDecrease) return true;
                }
                return false;
            })
            .forEach(record -> {
                this.decreasePower(record.getPlayerUUID());
            });
    }

}
