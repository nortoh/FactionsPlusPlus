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
import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionFlag;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
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
    private final EphemeralData ephemeralData;
    private final ConfigService configService;
    private final DataService dataService;
    private final FactionService factionService;

    private final String APIVersion = "v1.0.0"; // every time the external API is altered, this should be incremented

    @Inject
    public FactionsPlusPlusAPI(
        DataService dataService,
        FactionsPlusPlus factionsPlusPlus,
        EphemeralData ephemeralData,
        ConfigService configService,
        FactionService factionService
    ) {
        this.factionsPlusPlus = factionsPlusPlus;
        this.ephemeralData = ephemeralData;
        this.configService = configService;
        this.dataService = dataService;
        this.factionService = factionService;
    }

    public String getAPIVersion() {
        return this.APIVersion;
    }

    public String getVersion() {
        return this.factionsPlusPlus.getVersion();
    }

    public FPP_Faction getFaction(String factionName) {
        Faction faction = this.dataService.getFaction(factionName);
        if (faction == null) {
            return null;
        }
        return new FPP_Faction(faction);
    }

    public FPP_Faction getFaction(Player player) {
        Faction faction = this.dataService.getPlayersFaction(player.getUniqueId());
        if (faction == null) {
            return null;
        }
        return new FPP_Faction(faction);
    }

    public FPP_Faction getFaction(UUID playerUUID) {
        Faction faction = this.dataService.getPlayersFaction(playerUUID);

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
        for (Faction faction : this.dataService.getFactionRepository().all().values()) factions.add(new FPP_Faction(faction));
        return factions;
    }

    public boolean isPlayerInFactionChat(Player player) {
        return this.ephemeralData.isPlayerInFactionChat(player);
    }

    public boolean isPrefixesFeatureEnabled() {
        return this.configService.getBoolean("playersChatWithPrefixes");
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return (this.dataService.getClaimedChunk(chunk) != null);
    }

    public double getPower(Player player) {
        return this.dataService.getPlayerRecord(player.getUniqueId()).getPower();
    }

    public double getPower(UUID playerUUID) {
        return this.dataService.getPlayerRecord(playerUUID).getPower();
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
        PlayerRecord record = this.dataService.getPlayerRecord(player.getUniqueId());
        double originalPower = record.getPower();
        double newPower = originalPower + amount;
        record.setPower(newPower);
    }

    public void decreasePower(Player player, int amount) {
        PlayerRecord record = this.dataService.getPlayerRecord(player.getUniqueId());
        double originalPower = record.getPower();
        double newPower = originalPower - amount;
        if (newPower >= 0) record.setPower(originalPower - amount);
        else record.setPower(0);
    }
}