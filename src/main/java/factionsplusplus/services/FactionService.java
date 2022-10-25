package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.utils.Pair;
import factionsplusplus.utils.PlayerUtils;
import factionsplusplus.utils.Comparators;
import factionsplusplus.utils.Logger;
import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.builders.MultiMessageBuilder;
import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import factionsplusplus.models.Faction;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.repositories.PlayerRecordRepository;
import factionsplusplus.repositories.ClaimedChunkRepository;
import factionsplusplus.repositories.LockedBlockRepository;

import javax.inject.Provider;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
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
        Logger logger
    ) {
        this.configService = configService;
        this.factionRepository = factionRepository;
        this.playerRecordRepository = playerRecordRepository;
        this.playerService = playerService;
        this.dynmapService = dynmapService;
        this.lockedBlockRepository = lockedBlockRepository;
        this.claimedChunkRepository = claimedChunkRepository;
        this.logger = logger;
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

        for (UUID playerUUID : faction.getMemberList()) {
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
        return new Faction(factionName, ownerUUID, this.factionRepository.getDefaultFlags());
    }

    public Faction createFaction(String factionName) {
        return new Faction(factionName, this.factionRepository.getDefaultFlags());
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