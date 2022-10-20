/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.externalapi;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Faction;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.ConfigService;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 * @brief This class gives developers access to the external API for Medieval Factions.
 */
@Singleton
public class FactionsPlusPlusAPI {
    private final FactionsPlusPlus factionsPlusPlus;
    private final PersistentData persistentData;
    private final EphemeralData ephemeralData;
    private final ConfigService configService;
    private final FactionRepository factionRepository;

    private final String APIVersion = "v1.0.0"; // every time the external API is altered, this should be incremented

    @Inject
    public FactionsPlusPlusAPI(FactionRepository factionRepository, FactionsPlusPlus factionsPlusPlus, PersistentData persistentData, EphemeralData ephemeralData, ConfigService configService) {
        this.factionsPlusPlus = factionsPlusPlus;
        this.persistentData = persistentData;
        this.ephemeralData = ephemeralData;
        this.configService = configService;
        this.factionRepository = factionRepository;
    }

    public String getAPIVersion() {
        return APIVersion;
    }

    public String getVersion() {
        return factionsPlusPlus.getVersion();
    }

    public FPP_Faction getFaction(String factionName) {
        Faction faction = this.factionRepository.get(factionName);
        if (faction == null) {
            return null;
        }
        return new FPP_Faction(faction);
    }

    public FPP_Faction getFaction(Player player) {
        Faction faction = this.persistentData.getPlayersFaction(player.getUniqueId());
        if (faction == null) {
            return null;
        }
        return new FPP_Faction(faction);
    }

    public FPP_Faction getFaction(UUID playerUUID) {
        Faction faction = this.persistentData.getPlayersFaction(playerUUID);
        if (faction == null) {
            return null;
        }
        return new FPP_Faction(faction);
    }

    public boolean isPlayerInFactionChat(Player player) {
        return ephemeralData.isPlayerInFactionChat(player);
    }

    public boolean isPrefixesFeatureEnabled() {
        return configService.getBoolean("playersChatWithPrefixes");
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return (persistentData.getChunkDataAccessor().getClaimedChunk(chunk) != null);
    }

    public double getPower(Player player) {
        return this.persistentData.getPlayerRecord(player.getUniqueId()).getPower();
    }

    public double getPower(UUID playerUUID) {
        return this.persistentData.getPlayerRecord(playerUUID).getPower();
    }

    public void forcePlayerToLeaveFactionChat(UUID uuid) {
        ephemeralData.getPlayersInFactionChat().remove(uuid);
    }

    public void increasePower(Player player, int amount) {
        PlayerRecord record = this.persistentData.getPlayerRecord(player.getUniqueId());
        double originalPower = record.getPower();
        double newPower = originalPower + amount;
        record.setPower(newPower);
    }

    public void decreasePower(Player player, int amount) {
        PlayerRecord record = this.persistentData.getPlayerRecord(player.getUniqueId());
        double originalPower = record.getPower();
        double newPower = originalPower - amount;
        if (newPower >= 0) {
            record.setPower(originalPower - amount);
        } else {
            record.setPower(0);
        }
    }
}