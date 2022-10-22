package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.utils.Pair;
import factionsplusplus.utils.PlayerUtils;
import factionsplusplus.utils.Comparators;
import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.builders.MultiMessageBuilder;
import factionsplusplus.builders.interfaces.GenericMessageBuilder;
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
import java.util.List;
import java.util.UUID;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    private final Map<String, FactionFlag> defaultFlags = new HashMap<>();

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
        this.initializeDefaultFactionFlags();
    }

    public void addDefaultFactionFlag(String flagName, FactionFlag flag) {
        this.defaultFlags.put(flagName, flag);
    }

    private void initializeDefaultFactionFlags() {
        this.addDefaultFactionFlag("mustBeOfficerToManageLand", new FactionFlag(FlagType.Boolean, true));
        this.addDefaultFactionFlag("mustBeOfficerToInviteOthers", new FactionFlag(FlagType.Boolean, true));
        this.addDefaultFactionFlag("alliesCanInteractWithLand", new FactionFlag(FlagType.Boolean, this.configService.getBoolean("allowAllyInteraction")));
        this.addDefaultFactionFlag("vassalageTreeCanInteractWithLand", new FactionFlag(FlagType.Boolean, this.configService.getBoolean("allowVassalageTreeInteraction")));
        this.addDefaultFactionFlag("neutral", new FactionFlag(FlagType.Boolean, false));
        this.addDefaultFactionFlag("dynmapTerritoryColor", new FactionFlag(FlagType.Color, "#ff0000"));
        this.addDefaultFactionFlag("territoryAlertColor", new FactionFlag(FlagType.Color, this.configService.getString("territoryAlertColor")));
        this.addDefaultFactionFlag("prefixColor", new FactionFlag(FlagType.Color, "white"));
        this.addDefaultFactionFlag("allowFriendlyFire", new FactionFlag(FlagType.Boolean, false));
        this.addDefaultFactionFlag("acceptBonusPower", new FactionFlag(FlagType.Boolean, true));
        this.addDefaultFactionFlag("enableMobProtection", new FactionFlag(FlagType.Boolean, true));
    }

    public void addFlagToMissingFactions(String flagName) {
        // get the flag from defaultFlags
        FactionFlag flag = this.defaultFlags.get(flagName);
        // TODO: error if null
        for (Faction faction : this.factionRepository.all().values()) {
            if (!faction.getFlags().containsKey(flagName)) faction.getFlags().put(flagName, flag);
        }
    }

    public void removeFlagFromFactions(String flagName) {
        // remove from default flags first
        this.defaultFlags.remove(flagName);
        // iterate through factions, removing the flag
        for (Faction faction : this.factionRepository.all().values()) faction.getFlags().remove(flagName);
    }

    public Map<String, FactionFlag> getDefaultFlags() {
        return this.defaultFlags;
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
            Faction vassalFaction = this.factionRepository.get(factionUUID);
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
        Faction topLiege = this.factionRepository.get(liegeUUID);
        while (topLiege != null) {
            topLiege = this.factionRepository.get(liegeUUID);
            if (topLiege != null) {
                liegeUUID = topLiege.getID();
            }
        }
        return liegeUUID;
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
        this.factionRepository.all().values()
            .stream()
            .filter(faction -> !faction.equals(targetFaction))
            .forEach(faction -> {
                faction.removeAlly(targetFaction.getID());
                faction.removeEnemy(targetFaction.getID());
                faction.unsetIfLiege(targetFaction.getID());
                faction.removeVassal(targetFaction.getID());
            });
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

    public void disbandAllZeroPowerFactions() {
        this.factionRepository.all().values()
            .stream()
            .filter(faction -> this.getCumulativePowerLevel(faction) == 0)
            .forEach(faction -> {
                // TODO: send "AlertDisbandmentDueToZeroPower" in some way to the faction
                this.removeFaction(faction);
            });
    }

    public long removeLiegeAndVassalReferencesToFaction(UUID factionUUID) {
        long changes = this.factionRepository.all().values().stream()
                .filter(f -> f.isLiege(factionUUID) || f.isVassal(factionUUID))
                .count(); // Count changes

        this.factionRepository.all().values().stream().filter(f -> f.isLiege(factionUUID)).forEach(f -> f.setLiege(null));
        this.factionRepository.all().values().stream().filter(f -> f.isVassal(factionUUID)).forEach(Faction::clearVassals);

        return changes;
    }

    public Collection<Faction> getFactionsByPower() {
        return this.factionRepository.all().values()
            .stream()
            .map(faction -> Pair.of(faction, this.getCumulativePowerLevel(faction)))
            .sorted(Comparators.FACTIONS_BY_POWER)
            .map(pair -> pair.left())
            .collect(Collectors.toList());
    }

    public List<Faction> getFactionsFromUUIDs(List<UUID> factionUUIDs) {
        return factionUUIDs.stream()
            .map(id -> this.factionRepository.get(id))
            .collect(Collectors.toList());
    }

    public String getCommaSeparatedFactionNames(List<UUID> factionUUIDs) {
        return this.getFactionsFromUUIDs(factionUUIDs)
            .stream()
            .map(Faction::toString)
            .collect(Collectors.joining(", "));
    }

    public GenericMessageBuilder generateFactionInfo(Faction faction) {
        MultiMessageBuilder builder = new MultiMessageBuilder();
        // Faction header
        builder.add(new MessageBuilder("FactionInfo.Title"));
        // Faction name
        builder.add(new MessageBuilder("FactionInfo.Name").with("name", faction.getName()));
        // Owner
        builder.add(new MessageBuilder("FactionInfo.Owner").with("owner", PlayerUtils.parseAsPlayer(faction.getOwner()).getName()));
        // Description (if applicable)
        if (faction.getDescription() != null) builder.add(new MessageBuilder("FactionInfo.Description").with("desc", faction.getDescription()));
        // Population
        builder.add(new MessageBuilder("FactionInfo.Population").with("amount", String.valueOf(faction.getPopulation())));
        // Allies (if applicable)
        if (!faction.getAllies().isEmpty()) builder.add(new MessageBuilder("FactionInfo.Allies").with("factions", String.join(", ", this.getCommaSeparatedFactionNames(faction.getAllies()))));
        // Enemies (if applicable)
        if (!faction.getEnemyFactions().isEmpty()) builder.add(new MessageBuilder("FactionInfo.AtWarWith").with("factions", String.join(", ", this.getCommaSeparatedFactionNames(faction.getEnemyFactions()))));
        // Power level
        final int claimedChunks = this.claimedChunkRepository.getAllForFaction(faction).size();
        final int cumulativePowerLevel = this.getCumulativePowerLevel(faction);
        builder.add(new MessageBuilder("FactionInfo.PowerLevel").with("level", String.valueOf(cumulativePowerLevel)).with("max", String.valueOf(this.getMaximumCumulativePowerLevel(faction))));
        // Demesne Size
        builder.add(new MessageBuilder("FactionInfo.DemesneSize").with("number", String.valueOf(claimedChunks)).with("max", String.valueOf(cumulativePowerLevel)));

        // Bonus power enabled?
        if (faction.getBonusPower() != 0) builder.add(new MessageBuilder("BonusPower").with("amount", String.valueOf(faction.getBonusPower())));

        // Is Vassal?
        if (faction.hasLiege()) {
            Faction liege = this.factionRepository.get(faction.getLiege());
            if (liege != null) builder.add(new MessageBuilder("Liege").with("name", liege.getName()));
        }

        // Is Liege?
        if (faction.isLiege()) {
            int vassalContribution = this.calculateCumulativePowerLevelWithVassalContribution(faction) - this.calculateCumulativePowerLevelWithoutVassalContribution(faction);
            if (this.isWeakened(faction)) vassalContribution = 0;
            builder.add(new MessageBuilder("Vassals").with("name", this.getCommaSeparatedFactionNames(faction.getVassals())));
            builder.add(new MessageBuilder("VassalContribution").with("amount", String.valueOf(vassalContribution)));
        }

        // Send off!
        return builder;
    }
}