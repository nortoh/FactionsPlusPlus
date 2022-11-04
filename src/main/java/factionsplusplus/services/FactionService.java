package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.utils.Pair;
import factionsplusplus.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import factionsplusplus.utils.Comparators;
import factionsplusplus.utils.Logger;
import factionsplusplus.data.factories.FactionFactory;
import factionsplusplus.data.repositories.ClaimedChunkRepository;
import factionsplusplus.data.repositories.FactionRepository;
import factionsplusplus.data.repositories.GateRepository;
import factionsplusplus.data.repositories.LockedBlockRepository;
import factionsplusplus.data.repositories.PlayerRecordRepository;
import factionsplusplus.models.Faction;
import factionsplusplus.models.ConfigurationFlag;

import javax.inject.Provider;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@Singleton
public class FactionService {
    private final ConfigService configService;
    private final FactionRepository factionRepository;
    private final PlayerRecordRepository playerRecordRepository;
    private final PlayerService playerService;
    private final Provider<DynmapIntegrationService> dynmapService;
    private final LockedBlockRepository lockedBlockRepository;
    private final ClaimedChunkRepository claimedChunkRepository;
    private final FactionFactory factionFactory;
    private final GateRepository gateRepository;
    private final Logger logger;

    @Inject
    public FactionService(
        ConfigService configService,
        FactionRepository factionRepository,
        PlayerRecordRepository playerRecordRepository,
        PlayerService playerService,
        Provider<DynmapIntegrationService> dynmapService,
        LockedBlockRepository lockedBlockRepository,
        ClaimedChunkRepository claimedChunkRepository,
        GateRepository gateRepository,
        Logger logger,
        FactionFactory factionFactory
    ) {
        this.configService = configService;
        this.factionRepository = factionRepository;
        this.playerRecordRepository = playerRecordRepository;
        this.playerService = playerService;
        this.dynmapService = dynmapService;
        this.lockedBlockRepository = lockedBlockRepository;
        this.claimedChunkRepository = claimedChunkRepository;
        this.logger = logger;
        this.factionFactory = factionFactory;
        this.gateRepository = gateRepository;
    }

    public void addDefaultConfigurationFlag(String flagName, ConfigurationFlag flag) {
        this.factionRepository.addDefaultConfigurationFlag(flagName, flag);
    }

    public void addFlagToMissingFactions(String flagName) {
        this.factionRepository.addFlagToMissingFactions(flagName);
    }

    public void removeFlagFromFactions(String flagName) {
        this.factionRepository.removeFlagFromFactions(flagName);
    }

    public void setBonusPower(Faction faction, int power) {
        if (! this.configService.getBoolean("bonusPowerEnabled") || ! (faction.getFlag("acceptBonusPower").toBoolean())) {
            return;
        }
        faction.setBonusPower(power);
    }

    public int calculateMaxOfficers(Faction faction) {
        int officersPerXNumber = this.configService.getInt("officerPerMemberCount");
        int officersFromConfig = faction.getMembers().size() / officersPerXNumber;
        return 1 + officersFromConfig;
    }

    public int calculateCumulativePowerLevelWithoutVassalContribution(Faction faction) {
        int powerLevel = 0;
        for (UUID playerUUID : faction.getMembers().keySet()) {
            try {
                powerLevel += this.playerRecordRepository.get(playerUUID).getPower();
            } catch (Exception e) {
                this.logger.error(e.getMessage(), e);
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

        for (UUID playerUUID : faction.getMembers().keySet()) {
            try {
                maxPower += this.playerService.getMaxPower(playerUUID);
            } catch (Exception e) {
                this.logger.error(e.getMessage(), e);
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
        return this.factionFactory.create(factionName, ownerUUID, this.factionRepository.getDefaultFlags());
    }

    public Faction createFaction(String factionName) {
        return this.factionFactory.create(factionName, this.factionRepository.getDefaultFlags());
    }

    public void removeFaction(Faction faction) {
        // SQL will handle this for us but for persistence sake, we need to update our local cache too.
        this.unclaimAllClaimedChunks(faction);
        this.removeAllOwnedLocks(faction);
        this.removePoliticalTiesToFaction(faction);
        this.removeAllOwnedGates(faction);
        this.factionRepository.delete(faction);
    }

    public void unclaimAllClaimedChunks(Faction faction) {
        this.claimedChunkRepository.getAllForFaction(faction).stream().forEach(this.claimedChunkRepository::remove);
        this.dynmapService.get().updateClaimsIfAble();
    }

    public void removePoliticalTiesToFaction(Faction targetFaction) {
        this.factionRepository.all().values()
            .stream()
            .filter(faction -> ! faction.equals(targetFaction))
            .forEach(faction -> {
                faction.removeAlly(targetFaction.getID());
                faction.removeEnemy(targetFaction.getID());
                faction.unsetIfLiege(targetFaction.getID());
                faction.removeVassal(targetFaction.getID());
            });
    }

    public void removeAllBases(Faction faction) {
        faction.getBases().keySet().stream().forEach(faction::removeBase);
    }

    public void removeAllOwnedGates(Faction faction) {
        this.gateRepository.getAllForFaction(faction.getUUID()).stream().forEach(this.gateRepository::remove);
    }

    public void removeAllOwnedLocks(Faction faction) {
        this.lockedBlockRepository.getAllForFaction(faction.getUUID()).stream().forEach(this.lockedBlockRepository::remove);
    }

    public void disbandAllZeroPowerFactions() {
        this.factionRepository.all().values()
            .stream()
            .filter(faction -> this.getCumulativePowerLevel(faction) == 0)
            .forEach(faction -> {
                faction.alert("FactionNotice.Disbandment.ZeroPower");
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
    
    // TODO: new messaging api
    public List<ComponentLike> generateFactionInfo(Faction faction) {
        List<ComponentLike> factionInfo = new ArrayList<>();
        // Header
        factionInfo.add(Component.translatable("FactionInfo.Title"));
        // Name
        factionInfo.add(Component.translatable("FactionInfo.Name").args(Component.text(faction.getName())));
        // Owner
        factionInfo.add(Component.translatable("FactionInfo.Owner").args(Component.text(PlayerUtils.parseAsPlayer(faction.getOwner().getUUID()).getName())));
        // Description (if applicable)
        if (faction.getDescription() != null) factionInfo.add(Component.translatable("FactionInfo.Description").args(Component.text(faction.getDescription())));
        // Population
        factionInfo.add(Component.translatable("FactionInfo.Population").args(Component.text(faction.getMemberCount())));
        // Allies (if applicable)
        if (! faction.getAllies().isEmpty()) factionInfo.add(Component.translatable("FactionInfo.Allies").args(Component.text(this.getCommaSeparatedFactionNames(faction.getAllies()))));
        // Enemies (if applicable)
        if (! faction.getEnemies().isEmpty()) factionInfo.add(Component.translatable("FactionInfo.Enemies").args(Component.text(this.getCommaSeparatedFactionNames(faction.getEnemies()))));
        // Power Level
        final int claimedChunks = this.claimedChunkRepository.getAllForFaction(faction).size();
        final int cumulativePowerLevel = this.getCumulativePowerLevel(faction);
        factionInfo.add(Component.translatable("FactionInfo.PowerLevel").args(Component.text(cumulativePowerLevel), Component.text(this.getMaximumCumulativePowerLevel(faction))));
        // Demesne Size
        factionInfo.add(Component.translatable("FactionInfo.DemesneSize").args(Component.text(claimedChunks), Component.text(cumulativePowerLevel)));
        // Bonus Power (if applicable)
        if (faction.getBonusPower() != 0) factionInfo.add(Component.translatable("FactionInfo.BonusPower").args(Component.text(faction.getBonusPower())));
        // Liege (if applicable)
        if (faction.hasLiege()) factionInfo.add(Component.translatable("FactionInfo.Liege").args(Component.text(this.factionRepository.get(faction.getLiege()).getName())));
        // Vassals (if applicable)
        if (faction.isLiege()) {
            int vassalContribution = this.calculateCumulativePowerLevelWithVassalContribution(faction) - this.calculateCumulativePowerLevelWithoutVassalContribution(faction);
            if (this.isWeakened(faction)) vassalContribution = 0;
            factionInfo.add(Component.translatable("FactionInfo.Vassals").args(Component.text(this.getCommaSeparatedFactionNames(faction.getVassals()))));
            factionInfo.add(Component.translatable("FactionInfo.VassalContribution").args(Component.text(vassalContribution)));
        }
        return factionInfo;
    }
}