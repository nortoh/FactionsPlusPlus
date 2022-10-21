/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.externalapi;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.constants.FlagType;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionFlag;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.FactionService;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
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
    private final FactionService factionService;

    private final String APIVersion = "v1.0.0"; // every time the external API is altered, this should be incremented

    @Inject
    public FactionsPlusPlusAPI(
        FactionRepository factionRepository,
        FactionsPlusPlus factionsPlusPlus,
        PersistentData persistentData,
        EphemeralData ephemeralData,
        ConfigService configService,
        FactionService factionService
    ) {
        this.factionsPlusPlus = factionsPlusPlus;
        this.persistentData = persistentData;
        this.ephemeralData = ephemeralData;
        this.configService = configService;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
    }

    public String getAPIVersion() {
        return this.APIVersion;
    }

    public String getVersion() {
        return this.factionsPlusPlus.getVersion();
    }

    public FPP_Faction getFaction(String factionName) {
        Faction faction = this.factionRepository.get(factionName);
        if (faction == null) return null;
        return new FPP_Faction(faction);
    }

    public FPP_Faction getFaction(UUID factionUUID) {
        Faction faction = this.factionRepository.getByID(factionUUID);
        if (faction == null) return null;
        return new FPP_Faction(faction);
    }

    public FPP_Faction getFactionFromPlayer(UUID playerUUID) {
        Faction faction = this.persistentData.getPlayersFaction(playerUUID);
        if (faction == null) {
            return null;
        }
        return new FPP_Faction(faction);
    }

    public FPP_Faction getFactionFromPlayer(Player player) {
        return this.getFaction(player.getUniqueId());
    }

    public List<FPP_Faction> getFactions() {
        ArrayList<FPP_Faction> factions = new ArrayList<>();
        for (Faction faction : this.factionRepository.all().values()) factions.add(new FPP_Faction(faction));
        return factions;
    }

    public boolean isPlayerInFactionChat(Player player) {
        return this.ephemeralData.isPlayerInFactionChat(player);
    }

    public boolean isPrefixesFeatureEnabled() {
        return this.configService.getBoolean("playersChatWithPrefixes");
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return (this.persistentData.getChunkDataAccessor().getClaimedChunk(chunk) != null);
    }

    public double getPlayerPower(UUID playerUUID) {
        return this.persistentData.getPlayerRecord(playerUUID).getPower();
    }

    public double getPlayerPower(Player player) {
        return this.getPlayerPower(player.getUniqueId());
    }

    public void forcePlayerToLeaveFactionChat(UUID uuid) {
        this.ephemeralData.getPlayersInFactionChat().remove(uuid);
    }

    public boolean hasFactionFlag(String flagName) {
        return this.factionService.getDefaultFlags().containsKey(flagName);
    }

    public void createFactionFlag(String flagName, FlagType flagType, Object defaultValue) {
        // TODO: handle the flag name already existing  
        // Create the flag object
        FactionFlag flag = new FactionFlag(flagType, defaultValue);
        // Add to default flags for new factions
        this.factionService.addDefaultFactionFlag(flagName, flag);
        // Add to existing factions
        this.factionService.addFlagToMissingFactions(flagName);
    }

    public void deleteFactionFlag(String flagName) {
        // TODO: don't allow deleting core flags
        // TODO: handle flag not existing
        // Remove from factions and defaults
        this.factionService.removeFlagFromFactions(flagName);
    }

    public void increasePlayerPower(Player player, int amount) {
        PlayerRecord record = this.persistentData.getPlayerRecord(player.getUniqueId());
        double originalPower = record.getPower();
        double newPower = originalPower + amount;
        record.setPower(newPower);
    }

    public void decreasePlayerPower(Player player, int amount) {
        PlayerRecord record = this.persistentData.getPlayerRecord(player.getUniqueId());
        double originalPower = record.getPower();
        double newPower = originalPower - amount;
        if (newPower >= 0) record.setPower(originalPower - amount);
        else record.setPower(0);
    }
}