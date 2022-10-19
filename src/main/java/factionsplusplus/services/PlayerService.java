package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.constants.FactionRank;
import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Faction;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.repositories.PlayerRecordRepository;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;
import java.util.UUID;

/**
 * Sends messages to players and the console.
 */
@Singleton
public class PlayerService {
    @Inject private ConfigService configService;
    @Inject private PersistentData persistentData;
    @Inject private FactionRepository factionRepository;
    @Inject private PlayerRecordRepository playerRecordRepository;

    /**
     * Method to obtain a Player faction from an object.
     * <p>
     * This method can accept a UUID, Player, OfflinePlayer and a String (name or UUID).<br>
     * If the type isn't found, an exception is thrown.
     * </p>
     *
     * @param object to obtain the Player faction from.
     * @return {@link Faction}
     * @throws IllegalArgumentException when the object isn't compatible.
     */
    @SuppressWarnings("deprecation")
    public Faction getPlayerFaction(Object object) {
        if (object instanceof OfflinePlayer) {
            return this.persistentData.getPlayersFaction(((OfflinePlayer) object).getUniqueId());
        } else if (object instanceof UUID) {
            return this.persistentData.getPlayersFaction((UUID) object);
        } else if (object instanceof String) {
            try {
                return this.persistentData.getPlayersFaction(UUID.fromString((String) object));
            } catch (Exception e) {
                OfflinePlayer player = Bukkit.getOfflinePlayer((String) object);
                if (player.hasPlayedBefore()) {
                    return this.persistentData.getPlayersFaction(player.getUniqueId());
                }
            }
        }
        throw new IllegalArgumentException(object + " cannot be transferred into a Player");
    }

    public FactionRank getFactionRank(UUID playerUUID) {
        Faction playerFaction = this.getPlayerFaction(playerUUID);
        if (playerFaction != null) {
            if (playerFaction.getOwner().equals(playerUUID)) {
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
        PlayerRecord playerRecord = this.playerRecordRepository.get(playerUUID);
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
        PlayerRecord playerRecord = this.playerRecordRepository.get(playerUUID);
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

}
