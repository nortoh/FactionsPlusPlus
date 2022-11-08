package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.utils.Pair;
import factionsplusplus.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import factionsplusplus.utils.Comparators;
import factionsplusplus.data.factories.FactionFactory;
import factionsplusplus.data.repositories.ClaimedChunkRepository;
import factionsplusplus.data.repositories.FactionRepository;
import factionsplusplus.data.repositories.GateRepository;
import factionsplusplus.data.repositories.LockedBlockRepository;
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
    private final Provider<DynmapIntegrationService> dynmapService;
    private final LockedBlockRepository lockedBlockRepository;
    private final ClaimedChunkRepository claimedChunkRepository;
    private final FactionFactory factionFactory;
    private final GateRepository gateRepository;

    @Inject
    public FactionService(
        ConfigService configService,
        FactionRepository factionRepository,
        Provider<DynmapIntegrationService> dynmapService,
        LockedBlockRepository lockedBlockRepository,
        ClaimedChunkRepository claimedChunkRepository,
        GateRepository gateRepository,
        FactionFactory factionFactory
    ) {
        this.configService = configService;
        this.factionRepository = factionRepository;
        this.dynmapService = dynmapService;
        this.lockedBlockRepository = lockedBlockRepository;
        this.claimedChunkRepository = claimedChunkRepository;
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
                faction.removeAlly(targetFaction.getUUID());
                faction.removeEnemy(targetFaction.getUUID());
                faction.unsetIfLiege(targetFaction.getUUID());
                faction.removeVassal(targetFaction.getUUID());
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
            .filter(faction -> faction.getCumulativePowerLevel() == 0)
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
            .map(faction -> Pair.of(faction, faction.getCumulativePowerLevel()))
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
        final double cumulativePowerLevel = faction.getCumulativePowerLevel();
        factionInfo.add(Component.translatable("FactionInfo.PowerLevel").args(Component.text(cumulativePowerLevel), Component.text(faction.getMaximumCumulativePowerLevel())));
        // Demesne Size
        factionInfo.add(Component.translatable("FactionInfo.DemesneSize").args(Component.text(claimedChunks), Component.text(cumulativePowerLevel)));
        // Bonus Power (if applicable)
        if (faction.getBonusPower() != 0) factionInfo.add(Component.translatable("FactionInfo.BonusPower").args(Component.text(faction.getBonusPower())));
        // Liege (if applicable)
        if (faction.hasLiege()) factionInfo.add(Component.translatable("FactionInfo.Liege").args(Component.text(faction.getLiege().getName())));
        // Vassals (if applicable)
        if (faction.isLiege()) {
            double vassalContribution = faction.calculateCumulativePowerLevelWithVassalContribution() - faction.calculateCumulativePowerLevelWithoutVassalContribution();
            if (faction.isWeakened()) vassalContribution = 0;
            factionInfo.add(Component.translatable("FactionInfo.Vassals").args(Component.text(this.getCommaSeparatedFactionNames(faction.getVassals()))));
            factionInfo.add(Component.translatable("FactionInfo.VassalContribution").args(Component.text(vassalContribution)));
        }
        return factionInfo;
    }
}