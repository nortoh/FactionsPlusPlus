package factionsplusplus.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import factionsplusplus.models.Faction;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Command;
import factionsplusplus.models.ConfigOption;
import factionsplusplus.repositories.*;

import java.util.stream.Collectors;


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
        return this.claimedChunkRepository.getAllForFaction(faction);
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