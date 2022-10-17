package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.constants.FlagType;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.FactionFlag;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.repositories.PlayerRecordRepository;

import java.util.HashMap;
import java.util.UUID;
@Singleton
public class FactionService {
    private final ConfigService configService;
    private final FactionRepository factionRepository;
    private final PlayerRecordRepository playerRecordRepository;
    private final PlayerService playerService;

    @Inject
    public FactionService(
        ConfigService configService,
        FactionRepository factionRepository,
        PlayerRecordRepository playerRecordRepository,
        PlayerService playerService
    ) {
        this.configService = configService;
        this.factionRepository = factionRepository;
        this.playerRecordRepository = playerRecordRepository;
        this.playerService = playerService;
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
        for (String factionName : faction.getVassals()) {
            Faction vassalFaction = this.factionRepository.get(factionName);
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

    public String getTopLiege(Faction faction) 
    {
        String liegeName = faction.getLiege();
        Faction topLiege = this.factionRepository.get(liegeName);
        while (topLiege != null) {
            topLiege = this.factionRepository.get(topLiege.getLiege());
            if (topLiege != null) {
                liegeName = topLiege.getName();
            }
        }
        return liegeName;
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
}