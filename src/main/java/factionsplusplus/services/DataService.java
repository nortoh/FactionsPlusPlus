package factionsplusplus.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.Collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.Chunk;

import factionsplusplus.constants.FlagType;
import factionsplusplus.models.Faction;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.models.World;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Command;
import factionsplusplus.models.ConfigOption;
import factionsplusplus.models.ConfigurationFlag;
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
    private final WorldRepository worldRepository;
    private final ConfigService configService;

    @Inject
    public DataService(
        FactionRepository factionRepository,
        ClaimedChunkRepository claimedChunkRepository,
        LockedBlockRepository lockedBlockRepository,
        PlayerRecordRepository playerRecordRepository,
        CommandRepository commandRepository,
        ConfigOptionRepository configOptionRepository,
        WarRepository warRepository,
        WorldRepository worldRepository,
        ConfigService configService
    ) {
        this.factionRepository = factionRepository;
        this.claimedChunkRepository = claimedChunkRepository;
        this.lockedBlockRepository = lockedBlockRepository;
        this.playerRecordRepository = playerRecordRepository;
        this.commandRepository = commandRepository;
        this.configOptionRepository = configOptionRepository;
        this.warRepository = warRepository;
        this.worldRepository = worldRepository;
        this.configService = configService;
        this.initializeDefaultConfigurationFlags();
    }

    public void save() {
        this.worldRepository.persist();
        this.factionRepository.persist();
        this.claimedChunkRepository.persist();
        this.playerRecordRepository.persist();
        this.lockedBlockRepository.persist();
        this.warRepository.persist();
        if (this.configService.hasBeenAltered()) this.configService.saveConfigDefaults();
    }

    public void load() {
        this.worldRepository.load();
        this.factionRepository.load();
        this.claimedChunkRepository.load();
        this.playerRecordRepository.load();
        this.lockedBlockRepository.load();
        this.warRepository.load();
        this.initializeWorlds();
    }

    private void initializeWorlds() {
        Bukkit.getWorlds().stream()
            .forEach(world -> {
                if (this.worldRepository.get(world) == null) {
                    World newWorld = new World(world.getUID());
                    this.worldRepository.create(newWorld);
                }
            });
        this.worldRepository.addAnyMissingFlags();
    }

    private void initializeDefaultConfigurationFlags() {
        // Factions
        this.factionRepository.addDefaultConfigurationFlag("mustBeOfficerToManageLand", new ConfigurationFlag(FlagType.Boolean, true), false);
        this.factionRepository.addDefaultConfigurationFlag("mustBeOfficerToInviteOthers", new ConfigurationFlag(FlagType.Boolean, true), false);
        this.factionRepository.addDefaultConfigurationFlag("alliesCanInteractWithLand", new ConfigurationFlag(FlagType.Boolean, this.configService.getBoolean("allowAllyInteraction")), false);
        this.factionRepository.addDefaultConfigurationFlag("vassalageTreeCanInteractWithLand", new ConfigurationFlag(FlagType.Boolean, this.configService.getBoolean("allowVassalageTreeInteraction")), false);
        this.factionRepository.addDefaultConfigurationFlag("neutral", new ConfigurationFlag(FlagType.Boolean, false), false);
        this.factionRepository.addDefaultConfigurationFlag("dynmapTerritoryColor", new ConfigurationFlag(FlagType.Color, "#ff0000"), false);
        this.factionRepository.addDefaultConfigurationFlag("territoryAlertColor", new ConfigurationFlag(FlagType.Color, this.configService.getString("territoryAlertColor")), false);
        this.factionRepository.addDefaultConfigurationFlag("prefixColor", new ConfigurationFlag(FlagType.Color, "white"), false);
        this.factionRepository.addDefaultConfigurationFlag("allowFriendlyFire", new ConfigurationFlag(FlagType.Boolean, false), false);
        this.factionRepository.addDefaultConfigurationFlag("acceptBonusPower", new ConfigurationFlag(FlagType.Boolean, true), false);
        this.factionRepository.addDefaultConfigurationFlag("enableMobProtection", new ConfigurationFlag(FlagType.Boolean, true), false);

        // Worlds
        this.worldRepository.addDefaultConfigurationFlag("enabled", new ConfigurationFlag(FlagType.Boolean, true), false);
        this.worldRepository.addDefaultConfigurationFlag("allowClaims", new ConfigurationFlag(FlagType.Boolean, true), false);
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
        List<Faction> valuesList = new ArrayList<Faction>(this.factionRepository.all().values());
        return valuesList.get(new Random().nextInt(valuesList.size()));
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

    public ClaimedChunk getClaimedChunk(double x, double z, UUID world) {
        return this.claimedChunkRepository.get(x, z, world);
    }

    public ClaimedChunk getClaimedChunk(Block block) {
        return this.claimedChunkRepository.get(block.getX(), block.getZ(), block.getWorld().getUID());
    }

    public ClaimedChunk getClaimedChunk(Chunk chunk) {
        return this.claimedChunkRepository.get(chunk.getX(), chunk.getZ(), chunk.getWorld().getUID());
    }

    public List<ClaimedChunk> getClaimedChunksForFaction(Faction faction) {
        return this.claimedChunkRepository.getAllForFaction(faction);
    }

    public boolean isChunkClaimed(double x, double z, UUID world) {
        return this.getClaimedChunk(x, z, world) != null;
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return this.getClaimedChunk(chunk) != null;
    }

    public LockedBlockRepository getLockedBlockRepository() {
        return this.lockedBlockRepository;
    }

    public LockedBlock getLockedBlock(int x, int y, int z, UUID world) {
        return this.lockedBlockRepository.get(x, y, z, world);
    }

    public LockedBlock getLockedBlock(Block block) {
        return this.lockedBlockRepository.get(block.getX(), block.getY(), block.getZ(), block.getWorld().getUID());
    }

    public boolean isBlockLocked(Block block) {
        return this.getLockedBlock(block) != null;
    }

    public boolean isBlockLocked(int x, int y, int z, UUID world) {
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

    public WorldRepository getWorldRepository() {
        return this.worldRepository;
    }

    public World getWorld(String name) {
        return this.worldRepository.get(name);
    }

    public World getWorld(UUID uuid) {
        return this.worldRepository.get(uuid);
    }
}