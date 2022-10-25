package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.constants.FactionRank;
import factionsplusplus.models.Faction;
import factionsplusplus.models.PlayerRecord;

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

    public FactionRank getFactionRank(UUID playerUUID) {
        Faction playerFaction = this.dataService.getPlayersFaction(playerUUID);
        if (playerFaction != null) {
            if (playerFaction.getOwner().getUUID().equals(playerUUID)) {
                return FactionRank.Owner;
            } else if (playerFaction.isOfficer(playerUUID)) {
                return FactionRank.Officer;
            } else {
                return FactionRank.Member;
            }
        }
        return null;
    }

    public double getMaxPower(UUID playerUUID) {
        double initialPowerLevel = this.configService.getDouble("initialMaxPowerLevel");
        FactionRank rank = this.getFactionRank(playerUUID);
        if (rank == null) return initialPowerLevel;
        switch(this.getFactionRank(playerUUID)) {
            case Owner:
                return (int)(initialPowerLevel * this.configService.getDouble("factionOwnerMultiplier"));
            case Officer:
                return (int)(initialPowerLevel * this.configService.getDouble("factionOfficerMultiplier"));
            default:
                return initialPowerLevel;
        }
    }

    public double increasePowerBy(UUID playerUUID, int increaseAmount) {
        return this.increasePowerBy(playerUUID, (double)increaseAmount);
    }

    public double increasePowerBy(UUID playerUUID, double increaseAmount) {
        PlayerRecord playerRecord = this.dataService.getPlayerRecord(playerUUID);
        if (playerRecord == null) return 0;
        double currentPowerLevel = playerRecord.getPower();
        double maxPowerLevel = this.getMaxPower(playerUUID);
        if (currentPowerLevel < maxPowerLevel) {
            double newPowerLevel = currentPowerLevel + increaseAmount;
            playerRecord.setPower(Math.min(newPowerLevel, maxPowerLevel));
        }
        return playerRecord.getPower();
    }

    public double decreasePowerBy(UUID playerUUID, int decreaseAmount) {
        return this.decreasePowerBy(playerUUID, (double)decreaseAmount);
    }

    public double decreasePowerBy(UUID playerUUID, double decreaseAmount) {
        PlayerRecord playerRecord = this.dataService.getPlayerRecord(playerUUID);
        if (playerRecord == null) return 0;
        double currentPowerLevel = playerRecord.getPower();
        if (currentPowerLevel > 0) {
            double newPowerLevel = currentPowerLevel - decreaseAmount;
            playerRecord.setPower(Math.max(newPowerLevel, 0));
        }
        playerRecord.increasePowerLostBy(decreaseAmount);
        return playerRecord.getPower();
    }

    public void increasePower(UUID playerUUID) {
        this.increasePowerBy(playerUUID, this.configService.getInt("powerIncreaseAmount"));
    }

    public void decreasePower(UUID playerUUID) {
        this.decreasePowerBy(playerUUID, this.configService.getInt("powerDecreaseAmount"));
    }

    public void grantPowerDueToKill(UUID playerUUID) {
        this.increasePowerBy(playerUUID, this.configService.getDouble("powerGainedOnKill"));
    }

    public double revokePowerDueToDeath(UUID playerUUID) {
        double powerLost = this.configService.getDouble("powerLostOnDeath");
        this.decreasePowerBy(playerUUID, powerLost);
        return powerLost;
    }

    public void resetPowerLevels() {
        final int initialPowerLevel = this.configService.getInt("initialPowerLevel");
        this.dataService.getPlayerRecordRepository().all().forEach(record -> record.setPower(initialPowerLevel));
    }

    public void createActivityRecordForEveryOfflinePlayer() { // this method is to ensure that when updating to a version with power decay, even players who never log in again will experience power decay
        final int initialPowerLevel = this.configService.getInt("initialPowerLevel");
        Arrays.stream(Bukkit.getOfflinePlayers())
            .filter(player -> this.dataService.getPlayerRecord(player.getUniqueId()) == null)
            .forEach(player -> {
                PlayerRecord newRecord = new PlayerRecord(player.getUniqueId(), 1, initialPowerLevel);
                newRecord.setLastLogout(ZonedDateTime.now());
                this.dataService.getPlayerRecordRepository().create(newRecord);
            });
    }

    public void initiatePowerIncreaseForAllPlayers() {
        Bukkit.getOnlinePlayers()
            .stream()
            .map(player -> this.dataService.getPlayerRecord(player.getUniqueId()))
            .forEach(record -> this.initiatePowerIncrease(record));
    }

    private void initiatePowerIncrease(PlayerRecord record) {
        double maxPower = this.getMaxPower(record.getPlayerUUID());
        if (record.getPower() < maxPower && Objects.requireNonNull(getServer().getPlayer(record.getPlayerUUID())).isOnline()) {
            this.increasePower(record.getPlayerUUID());
            // TODO: re-implement messaging ALertPowerLevelIncreasedBY with amount being the config option as int "powerIncreaseAmount"
        }
    }

    public void decreasePowerForInactivePlayers() {
        if (! this.configService.getBoolean("powerDecreases")) return;
        final int minutesBeforePowerDecrease = this.configService.getInt("minutesBeforePowerDecrease");
        this.dataService.getPlayerRecords()
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
