package dansplugins.factionsystem.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.LockedBlock;
import dansplugins.factionsystem.models.PlayerRecord;
import dansplugins.factionsystem.models.ClaimedChunk;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.ConfigOption;
import dansplugins.factionsystem.repositories.*;


@Singleton
public class DataService {
    private final FactionRepository factionRepository;
    private final ClaimedChunkRepository claimedChunkRepository;
    private final LockedBlockRepository lockedBlockRepository;
    private final PlayerRecordRepository playerRecordRepository;
    private final CommandRepository commandRepository;
    private final ConfigOptionRepository configOptionRepository;

    @Inject
    public DataService(
        FactionRepository factionRepository,
        ClaimedChunkRepository claimedChunkRepository,
        LockedBlockRepository lockedBlockRepository,
        PlayerRecordRepository playerRecordRepository,
        CommandRepository commandRepository,
        ConfigOptionRepository configOptionRepository
    ) {
        this.factionRepository = factionRepository;
        this.claimedChunkRepository = claimedChunkRepository;
        this.lockedBlockRepository = lockedBlockRepository;
        this.playerRecordRepository = playerRecordRepository;
        this.commandRepository = commandRepository;
        this.configOptionRepository = configOptionRepository;
    }

    public FactionRepository getFactionRepository() {
        return this.factionRepository;
    }

    public Faction getFaction(String name) {
        return this.factionRepository.get(name);
    }

    public Faction getFactionByID(UUID uuid) {
        return this.factionRepository.getByID(uuid);
    }

    public Faction getRandomFaction() {
        return this.factionRepository.all().get(new Random().nextInt(this.factionRepository.all().size()));
    }

    public ClaimedChunkRepository getClaimedChunkRepository() {
        return this.claimedChunkRepository;
    }

    public ClaimedChunk getClaimedChunk(double x, double z, String world) {
        return this.claimedChunkRepository.get(x, z, world);
    }

    public LockedBlockRepository getLockedRepository() {
        return this.lockedBlockRepository;
    }

    public LockedBlock getLockedBlock(int x, int y, int z, String world) {
        return this.lockedBlockRepository.get(x, y, z, world);
    }

    public PlayerRecordRepository getPlayerRecordRepository() {
        return this.playerRecordRepository;
    }

    public PlayerRecord getPlayerRecord(UUID playerUUID) {
        return this.playerRecordRepository.get(playerUUID);
    }

    public List<ClaimedChunk> getClaimedChunksForFaction(Faction faction) {
        List<ClaimedChunk> chunks = new ArrayList<>();
        for (ClaimedChunk chunk : this.claimedChunkRepository.all()) {
            if (chunk.getHolder().equals(faction.getID())) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    public CommandRepository getCommandRepository() {
        return this.commandRepository;
    }

    public Command getCommand(String command) {
        return this.commandRepository.get(command);
    }

    public ConfigOptionRepository getConfigOptionRepository() {
        return this.configOptionRepository;
    }

    public ConfigOption getConfigOption(String optionName) {
        return this.configOptionRepository.get(optionName);
    }
}