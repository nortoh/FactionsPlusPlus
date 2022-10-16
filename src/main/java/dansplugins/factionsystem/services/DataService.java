package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.ClaimedChunk;

import dansplugins.factionsystem.repositories.ClaimedChunkRepository;
import dansplugins.factionsystem.repositories.FactionRepository;


@Singleton
public class DataService {
    private final FactionRepository factionRepository;
    private final ClaimedChunkRepository claimedChunkRepository;

    @Inject
    public DataService(
        FactionRepository factionRepository,
        ClaimedChunkRepository claimedChunkRepository
    ) {
        this.factionRepository = factionRepository;
        this.claimedChunkRepository = claimedChunkRepository;
    }

    public FactionRepository getFactionRepository() {
        return this.factionRepository;
    }

    public Faction getFaction(String name) {
        return this.factionRepository.get(name);
    }

    public ClaimedChunkRepository getClaimedChunkRepository() {
        return this.claimedChunkRepository;
    }

    public ClaimedChunk getClaimedChunk(double x, double z, String world) {
        return this.claimedChunkRepository.get(x, z, world);
    }
}