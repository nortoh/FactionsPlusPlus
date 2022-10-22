package factionsplusplus.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.Collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.Chunk;

import factionsplusplus.models.Faction;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Command;
import factionsplusplus.models.ConfigOption;
import factionsplusplus.models.Gate;
import factionsplusplus.repositories.*;


@Singleton
public class DataService {
    private final FactionRepository factionRepository;
    private final ClaimedChunkRepository claimedChunkRepository;
    private final LockedBlockRepository lockedBlockRepository;
    private final PlayerRecordRepository playerRecordRepository;
    private final CommandRepository commandRepository;
    private final ConfigOptionRepository configOptionRepository;
    private final WarRepository warRepository;

    @Inject
    public DataService(
        FactionRepository factionRepository,
        ClaimedChunkRepository claimedChunkRepository,
        LockedBlockRepository lockedBlockRepository,
        PlayerRecordRepository playerRecordRepository,
        CommandRepository commandRepository,
        ConfigOptionRepository configOptionRepository,
        WarRepository warRepository
    ) {
        this.factionRepository = factionRepository;
        this.claimedChunkRepository = claimedChunkRepository;
        this.lockedBlockRepository = lockedBlockRepository;
        this.playerRecordRepository = playerRecordRepository;
        this.commandRepository = commandRepository;
        this.configOptionRepository = configOptionRepository;
        this.warRepository = warRepository;
    }

    public void save() {
        this.factionRepository.persist();
        this.claimedChunkRepository.persist();
        this.playerRecordRepository.persist();
        this.lockedBlockRepository.persist();
        this.warRepository.persist();
        // TODO save config if it's been altered 
    }

    public void load() {
        this.factionRepository.load();
        this.claimedChunkRepository.load();
        this.playerRecordRepository.load();
        this.lockedBlockRepository.load();
        this.warRepository.load();
    }

    public FactionRepository getFactionRepository() {
        return this.factionRepository;
    }

    public Faction getFaction(String name) {
        return this.factionRepository.get(name);
    }

    public Faction getFaction(UUID uuid) {
        return this.factionRepository.get(uuid);
    }

    public Faction getPlayersFaction(OfflinePlayer player) {
        return this.factionRepository.getForPlayer(player);
    }

    public Faction getPlayersFaction(UUID playerUUID) {
        return this.factionRepository.getForPlayer(playerUUID);
    }

    public List<Faction> getFactionsInVassalageTree(Faction faction) {
        return this.factionRepository.getInVassalageTree(faction);
    }

    public Faction getGatesFaction(Gate gate) {
        return this.getFactions()
            .stream()
            .filter(faction -> faction.getGates().contains(gate))
            .findFirst()
            .orElse(null);
    }

    public boolean isPlayerInVassalageTree(OfflinePlayer player, Faction faction) {
        return this.getFactionsInVassalageTree(faction)
            .stream()
            .anyMatch(f -> f.isMember(player.getUniqueId()));
    }

    public boolean isPlayerInFaction(OfflinePlayer player) {
        return this.factionRepository.getForPlayer(player) != null;
    }

    public boolean isFactionPrefixTaken(String prefix) {
        return this.factionRepository.getByPrefix(prefix) != null;
    }

    public Faction getRandomFaction() {
        return this.factionRepository.all().get(new Random().nextInt(this.factionRepository.all().size()));
    }

    public int getNumberOfFactions() {
        return this.factionRepository.count();
    }

    public Collection<Faction> getFactions() {
        return this.factionRepository.all().values();
    }

    public Gate getGate(Block targetBlock) {
        return this.getFactions()
            .stream()
            .flatMap(faction -> faction.getGates().stream())
            .filter(gate -> gate.hasBlock(targetBlock))
            .findFirst()
            .orElse(null);
    }

    public boolean isGateBlock(Block targetBlock) {
        return this.getGate(targetBlock) != null;
    }

    public ClaimedChunkRepository getClaimedChunkRepository() {
        return this.claimedChunkRepository;
    }

    public ClaimedChunk getClaimedChunk(double x, double z, String world) {
        return this.claimedChunkRepository.get(x, z, world);
    }

    public ClaimedChunk getClaimedChunk(Block block) {
        return this.claimedChunkRepository.get(block.getX(), block.getZ(), block.getWorld().getName());
    }

    public ClaimedChunk getClaimedChunk(Chunk chunk) {
        return this.claimedChunkRepository.get(chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
    }

    public List<ClaimedChunk> getClaimedChunksForFaction(Faction faction) {
        return this.claimedChunkRepository.getAllForFaction(faction);
    }

    public boolean isChunkClaimed(double x, double z, String world) {
        return this.getClaimedChunk(x, z, world) != null;
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return this.getClaimedChunk(chunk) != null;
    }

    public LockedBlockRepository getLockedBlockRepository() {
        return this.lockedBlockRepository;
    }

    public LockedBlock getLockedBlock(int x, int y, int z, String world) {
        return this.lockedBlockRepository.get(x, y, z, world);
    }

    public LockedBlock getLockedBlock(Block block) {
        return this.lockedBlockRepository.get(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

    public boolean isBlockLocked(Block block) {
        return this.getLockedBlock(block) != null;
    }

    public boolean isBlockLocked(int x, int y, int z, String world) {
        return this.getLockedBlock(x, y, z, world) != null;
    }

    public PlayerRecordRepository getPlayerRecordRepository() {
        return this.playerRecordRepository;
    }

    public PlayerRecord getPlayerRecord(UUID playerUUID) {
        return this.playerRecordRepository.get(playerUUID);
    }

    public boolean hasPlayerRecord(OfflinePlayer player) {
        return this.getPlayerRecord(player.getUniqueId()) != null;
    }

    public boolean hasPlayerRecord(UUID uuid) {
        return this.getPlayerRecord(uuid) != null;
    }

    public int getNumberOfPlayers() {
        return this.playerRecordRepository.count();
    }

    public Collection<PlayerRecord> getPlayerRecords() {
        return this.playerRecordRepository.all();
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

    public WarRepository getWarRepository() {
        return this.warRepository;
    }
}