package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.constants.FlagType;
import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionFlag;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.repositories.PlayerRecordRepository;
import factionsplusplus.repositories.ClaimedChunkRepository;
import factionsplusplus.repositories.LockedBlockRepository;
import factionsplusplus.services.DynmapIntegrationService;

import javax.inject.Provider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.Map;

@Singleton
public class FactionService {
    private final ConfigService configService;
    private final FactionRepository factionRepository;
    private final PlayerRecordRepository playerRecordRepository;
    private final PlayerService playerService;
    private final Provider<DynmapIntegrationService> dynmapService;
    private final LockedBlockRepository lockedBlockRepository;
    private final ClaimedChunkRepository claimedChunkRepository;

    @Inject
    public FactionService(
        ConfigService configService,
        FactionRepository factionRepository,
        PlayerRecordRepository playerRecordRepository,
        PlayerService playerService,
        Provider<DynmapIntegrationService> dynmapService,
        LockedBlockRepository lockedBlockRepository,
        ClaimedChunkRepository claimedChunkRepository
    ) {
        this.configService = configService;
        this.factionRepository = factionRepository;
        this.playerRecordRepository = playerRecordRepository;
        this.playerService = playerService;
        this.dynmapService = dynmapService;
        this.lockedBlockRepository = lockedBlockRepository;
        this.claimedChunkRepository = claimedChunkRepository;
    }

    public void setBonusPower(Faction faction, int power) {
        if (!this.configService.getBoolean("bonusPowerEnabled") || !(faction.getFlag("acceptBonusPower").toBoolean())) {
            return;
        }
        faction.setBonusPower(power);
    }

    public int calculateMaxOfficers(Faction faction) {
        int officersPerXNumber = this.configService.getInt("officerPerMemberCount");
        int officersFromConfig = faction.getMemberList().size() / officersPerXNumber;
        return 1 + officersFromConfig;
    }

    public int calculateCumulativePowerLevelWithoutVassalContribution(Faction faction) {
        int powerLevel = 0;
        for (UUID playerUUID : faction.getMemberList()) {
            try {
                powerLevel += this.playerRecordRepository.get(playerUUID).getPower();
            } catch (Exception e) {
                // TODO: log this?
            }
        }
        return powerLevel;
    }

    public int calculateCumulativePowerLevelWithVassalContribution(Faction faction) {
        int vassalContribution = 0;
        double percentage = this.configService.getDouble("vassalContributionPercentageMultiplier");
        for (UUID factionUUID : faction.getVassals()) {
            Faction vassalFaction = this.factionRepository.getByID(factionUUID);
            if (vassalFaction != null) {
                vassalContribution += this.getCumulativePowerLevel(vassalFaction) * percentage;
            }
        }
        return calculateCumulativePowerLevelWithoutVassalContribution(faction) + vassalContribution;
    }

    public int getCumulativePowerLevel(Faction faction) {
        int withoutVassalContribution = this.calculateCumulativePowerLevelWithoutVassalContribution(faction);
        int withVassalContribution = this.calculateCumulativePowerLevelWithVassalContribution(faction);

        if (faction.getVassals().size() == 0 || (withoutVassalContribution < (getMaximumCumulativePowerLevel(faction) / 2))) {
            return withoutVassalContribution + faction.getBonusPower();
        } else {
            return withVassalContribution + faction.getBonusPower();
        }
    }

    public int getMaximumCumulativePowerLevel(Faction faction) {     // get max power without vassal contribution
        int maxPower = 0;

        for (UUID playerUUID : faction.getMemberList()) {
            try {
                maxPower += this.playerService.getMaxPower(playerUUID);
            } catch (Exception e) {
                // TODO: log this?
            }
        }
        return maxPower;
    }

    public boolean isWeakened(Faction faction) {
        return this.calculateCumulativePowerLevelWithoutVassalContribution(faction) < (this.getMaximumCumulativePowerLevel(faction) / 2);
    }

    public UUID getTopLiege(Faction faction) 
    {
        UUID liegeUUID = faction.getLiege();
        Faction topLiege = this.factionRepository.getByID(liegeUUID);
        while (topLiege != null) {
            topLiege = this.factionRepository.getByID(liegeUUID);
            if (topLiege != null) {
                liegeUUID = topLiege.getID();
            }
        }
        return liegeUUID;
    }

    public HashMap<String, FactionFlag> getDefaultFlags() {
        HashMap<String, FactionFlag> defaultFlags = new HashMap<>();
        defaultFlags.put("mustBeOfficerToManageLand", new FactionFlag(FlagType.Boolean, true));
        defaultFlags.put("mustBeOfficerToInviteOthers", new FactionFlag(FlagType.Boolean, true));
        defaultFlags.put("alliesCanInteractWithLand", new FactionFlag(FlagType.Boolean, this.configService.getBoolean("allowAllyInteraction")));
        defaultFlags.put("vassalageTreeCanInteractWithLand", new FactionFlag(FlagType.Boolean, this.configService.getBoolean("allowVassalageTreeInteraction")));
        defaultFlags.put("neutral", new FactionFlag(FlagType.Boolean, false));
        defaultFlags.put("dynmapTerritoryColor", new FactionFlag(FlagType.Color, "#ff0000"));
        defaultFlags.put("territoryAlertColor", new FactionFlag(FlagType.Color, this.configService.getString("territoryAlertColor")));
        defaultFlags.put("prefixColor", new FactionFlag(FlagType.Color, "white"));
        defaultFlags.put("allowFriendlyFire", new FactionFlag(FlagType.Boolean, false));
        defaultFlags.put("acceptBonusPower", new FactionFlag(FlagType.Boolean, true));
        defaultFlags.put("enableMobProtection", new FactionFlag(FlagType.Boolean, true));
        return defaultFlags;
    }
    
    public Faction createFaction(String factionName, UUID ownerUUID) {
        return new Faction(factionName, ownerUUID, this.getDefaultFlags());
    }

    public Faction createFaction(String factionName) {
        return new Faction(factionName, this.getDefaultFlags());
    }

    public void removeFaction(Faction faction) {
        this.unclaimAllClaimedChunks(faction);
        this.removeAllOwnedLocks(faction);
        this.removePoliticalTiesToFaction(faction);
        this.factionRepository.delete(faction);
    }

    public void unclaimAllClaimedChunks(Faction faction) {
        this.removeAllClaimedChunks(faction);
        this.dynmapService.get().updateClaimsIfAble();
    }

    public void removePoliticalTiesToFaction(Faction targetFaction) {
        for (Map.Entry<UUID, Faction> entry : this.factionRepository.all().entrySet()) {
            Faction faction = entry.getValue();
            faction.removeAlly(targetFaction.getID());
            faction.removeEnemy(targetFaction.getID());
            faction.unsetIfLiege(targetFaction.getID());
            faction.removeVassal(targetFaction.getID());
        }
    }
    public void removeAllClaimedChunks(Faction faction) {
        Iterator<ClaimedChunk> itr = this.claimedChunkRepository.all().iterator();
        while (itr.hasNext()) {
            ClaimedChunk currentChunk = itr.next();
            if (currentChunk.getHolder().equals(faction.getID())) itr.remove();
        }
    }
    public void removeAllOwnedLocks(Faction faction) {
        Iterator<LockedBlock> itr = this.lockedBlockRepository.all().iterator();

        while (itr.hasNext()) {
            LockedBlock currentBlock = itr.next();
            if (currentBlock.getFactionID().equals(faction.getID())) itr.remove();
        }
    }

}