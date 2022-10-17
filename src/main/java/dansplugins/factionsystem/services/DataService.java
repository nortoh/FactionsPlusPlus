package dansplugins.factionsystem.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.LockedBlock;
import dansplugins.factionsystem.models.PlayerRecord;
import dansplugins.factionsystem.models.ClaimedChunk;

import dansplugins.factionsystem.repositories.ClaimedChunkRepository;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.repositories.LockedBlockRepository;
import dansplugins.factionsystem.repositories.PlayerRecordRepository;


@Singleton
public class DataService {
    private final FactionRepository factionRepository;
    private final ClaimedChunkRepository claimedChunkRepository;
    private final LockedBlockRepository lockedBlockRepository;
    private final PlayerRecordRepository playerRecordRepository;

    @Inject
    public DataService(
        FactionRepository factionRepository,
        ClaimedChunkRepository claimedChunkRepository,
        LockedBlockRepository lockedBlockRepository,
        PlayerRecordRepository playerRecordRepository
    ) {
        this.factionRepository = factionRepository;
        this.claimedChunkRepository = claimedChunkRepository;
        this.lockedBlockRepository = lockedBlockRepository;
        this.playerRecordRepository = playerRecordRepository;
    }

    public FactionRepository getFactionRepository() {
        return this.factionRepository;
    }

    public Faction getFaction(String name) {
        return this.factionRepository.get(name);
    }

    public Faction getRandomFaction() {
        Random generator = new Random();
        int randomIndex = generator.nextInt(this.factionRepository.all().size());
        return this.factionRepository.all().get(randomIndex);
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
            if (chunk.getHolder().equalsIgnoreCase(faction.getName())) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }
}