package factionsplusplus.services;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.Collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jdbi.v3.core.Jdbi;
import org.bukkit.Chunk;

import factionsplusplus.constants.FlagDataType;
import factionsplusplus.constants.FlagType;
import factionsplusplus.data.daos.DefaultConfigurationFlagDao;
import factionsplusplus.data.repositories.*;
import factionsplusplus.models.Faction;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.models.FPPPlayer;
import factionsplusplus.models.World;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Command;
import factionsplusplus.models.ConfigOption;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.Gate;
import factionsplusplus.utils.BlockUtils;


@Singleton
public class DataService {
    private final DataProviderService dataProviderService;
    private final FactionRepository factionRepository;
    private final ClaimedChunkRepository claimedChunkRepository;
    private final LockedBlockRepository lockedBlockRepository;
    private final PlayerRepository playerRepository;
    private final CommandRepository commandRepository;
    private final ConfigOptionRepository configOptionRepository;
    private final WarRepository warRepository;
    private final WorldRepository worldRepository;
    private final GateRepository gateRepository;
    private final ConfigService configService;
    private Jdbi persistentData;

    @Inject
    public DataService(
        DataProviderService dataProviderService,
        FactionRepository factionRepository,
        ClaimedChunkRepository claimedChunkRepository,
        LockedBlockRepository lockedBlockRepository,
        PlayerRepository playerRepository,
        CommandRepository commandRepository,
        ConfigOptionRepository configOptionRepository,
        WarRepository warRepository,
        WorldRepository worldRepository,
        ConfigService configService,
        GateRepository gateRepository
    ) throws SQLException {
        this.dataProviderService = dataProviderService;
        this.factionRepository = factionRepository;
        this.claimedChunkRepository = claimedChunkRepository;
        this.lockedBlockRepository = lockedBlockRepository;
        this.playerRepository = playerRepository;
        this.commandRepository = commandRepository;
        this.configOptionRepository = configOptionRepository;
        this.warRepository = warRepository;
        this.worldRepository = worldRepository;
        this.configService = configService;
        this.gateRepository = gateRepository;
        this.persistentData = this.dataProviderService.getPersistentData();
    }

    /*
     * Saves all data in memory to disk. In most cases, data is written immediately. Data that is updated often may be saved using this function.
     */
    public void save() {
        this.playerRepository.persist(); // save player stats
        if (this.configService.hasBeenAltered()) this.configService.saveConfigDefaults();
    }

    public void disable() {
        this.dataProviderService.onDisable();
    }

    public void load() {
        this.initializeDefaultConfigurationFlags();
        this.worldRepository.load();
        this.factionRepository.load();
        this.claimedChunkRepository.load();
        this.playerRepository.load();
        this.lockedBlockRepository.load();
        this.gateRepository.load();
        this.warRepository.load();
    }

    private void addDefaultConfigurationFlag(ConfigurationFlag flag, FlagType flagScope) {
        this.persistentData.useExtension(DefaultConfigurationFlagDao.class, dao -> {
            dao.insert(flag.getName(), flagScope, flag);
        });
        switch(flagScope) {
            case Faction:
                this.factionRepository.addDefaultConfigurationFlag(flag.getName(), flag);
                break;
            case World:
                this.worldRepository.addDefaultConfigurationFlag(flag.getName(), flag);
                break;
        }
    }

    private void initializeDefaultConfigurationFlags() {
        // Factions
        this.addDefaultConfigurationFlag(new ConfigurationFlag("mustBeOfficerToManageLand", FlagDataType.Boolean, this.configService.getBoolean("faction.default.flags.mustBeOfficerToManageLand")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("mustBeOfficerToInviteOthers", FlagDataType.Boolean, this.configService.getBoolean("faction.default.flags.mustBeOfficerToInviteOthers")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("alliesCanInteractWithLand", FlagDataType.Boolean, this.configService.getBoolean("faction.default.flags.alliesCanInteractWithLand")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("vassalageTreeCanInteractWithLand", FlagDataType.Boolean, this.configService.getBoolean("faction.default.flags.vassalageTreeCanInteractWithLand")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("neutral", FlagDataType.Boolean, this.configService.getBoolean("faction.default.flags.neutral")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("public", FlagDataType.Boolean, this.configService.getBoolean("faction.default.flags.public")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("dynmapTerritoryColor", FlagDataType.Color, this.configService.getString("faction.default.flags.dynmapTerritoryColor")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("territoryIndicatorColor", FlagDataType.Color, this.configService.getString("faction.default.flags.territoryIndicatorColor")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("prefixColor", FlagDataType.Color, this.configService.getString("faction.default.flags.prefixColor")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("allowFriendlyFire", FlagDataType.Boolean, this.configService.getBoolean("faction.default.flags.allowFriendlyFire")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("acceptBonusPower", FlagDataType.Boolean, this.configService.getBoolean("faction.default.flags.acceptBonusPower")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("enableMobProtection", FlagDataType.Boolean, this.configService.getBoolean("faction.default.flags.enableMobProtection")), FlagType.Faction);

        // Worlds
        this.addDefaultConfigurationFlag(new ConfigurationFlag("enabled", FlagDataType.Boolean, this.configService.getBoolean("world.default.flags.enabled")), FlagType.World);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("allowClaims", FlagDataType.Boolean, this.configService.getBoolean("world.default.flags.allowClaims")), FlagType.World);
    }

    // Factions

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

    public void addFactionInvite(Faction faction, OfflinePlayer player) {
        this.factionRepository.getDAO().insertInvite(faction.getUUID(), player.getUniqueId());
    }

    public void removeFactionInvite(Faction faction, OfflinePlayer player) {
        this.factionRepository.getDAO().deleteInvite(faction.getUUID(), player.getUniqueId());
    }

    public boolean hasFactionInvite(Faction faction, OfflinePlayer player) {
        return this.factionRepository.getDAO().getInvite(faction.getUUID(), player.getUniqueId()) > 0;
    }

    // Gates

    public GateRepository getGateRepository() {
        return this.gateRepository;
    }

    public List<Gate> getGatesForFactionsTriggerBlock(UUID factionUUID, Block targetBlock) {
        return this.getGatesForTriggerBlock(targetBlock)
            .stream()
            .filter(g -> g.getFaction().equals(factionUUID))
            .toList();
    }

    public List<Gate> getGatesForTriggerBlock(Block targetBlock) {
        return this.gateRepository.getGatesForTriggerBlock(targetBlock);
    }

    public List<Gate> getFactionsGates(UUID factionUUID) {
        return this.gateRepository.getAllForFaction(factionUUID);
    }

    public List<Gate> getFactionsGates(Faction faction) {
        return this.getFactionsGates(faction.getUUID());
    }

    public Gate getGateWithBlock(Block targetBlock) {
        return this.gateRepository.getGateForBlock(targetBlock);
    }

    public boolean isGateBlock(Block targetBlock) {
        return this.getGateWithBlock(targetBlock) != null;
    }

    public void removeGate(Gate gate) {
        this.gateRepository.delete(gate);
    }

    // Claimed Chunks

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

    public void addClaimedChunk(ClaimedChunk chunk) {
        this.claimedChunkRepository.create(chunk);
    }

    public void deleteClaimedChunk(ClaimedChunk chunk) {
        this.claimedChunkRepository.delete(chunk);
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

    // Locked Blocks

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

    // Players

    public PlayerRepository getPlayerRepository() {
        return this.playerRepository;
    }

    public void createPlayer(FPPPlayer record) {
        this.playerRepository.create(record);
    }

    public FPPPlayer getPlayer(OfflinePlayer player) {
        return this.playerRepository.get(player.getUniqueId());
    }

    public FPPPlayer getPlayer(UUID playerUUID) {
        return this.playerRepository.get(playerUUID);
    }

    public boolean hasPlayer(OfflinePlayer player) {
        return this.playerRepository.contains(player.getUniqueId());
    }

    public boolean hasPlayer(UUID uuid) {
        return this.playerRepository.contains(uuid);
    }

    public int getNumberOfPlayers() {
        return this.playerRepository.count();
    }

    public Collection<FPPPlayer> getPlayers() {
        return this.playerRepository.all().values();
    }

    // Commands

    public CommandRepository getCommandRepository() {
        return this.commandRepository;
    }

    public Command getCommand(String command) {
        return this.commandRepository.get(command);
    }

    // Config Options

    public ConfigOptionRepository getConfigOptionRepository() {
        return this.configOptionRepository;
    }

    public ConfigOption getConfigOption(String optionName) {
        return this.configOptionRepository.get(optionName);
    }

    // Wars

    public WarRepository getWarRepository() {
        return this.warRepository;
    }

    // Worlds 

    public WorldRepository getWorldRepository() {
        return this.worldRepository;
    }

    public World getWorld(String name) {
        return this.worldRepository.get(name);
    }

    public World getWorld(UUID uuid) {
        return this.worldRepository.get(uuid);
    }

    // Utilities

    public boolean isBlockNextToNonOwnedLockedChest(OfflinePlayer player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
        Block neighbor3 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
        Block neighbor4 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);

        if (BlockUtils.isChest(neighbor1)) {
            if (this.isBlockLocked(neighbor1) && this.getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockUtils.isChest(neighbor2)) {
            if (this.isBlockLocked(neighbor2) && this.getLockedBlock(neighbor2).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockUtils.isChest(neighbor3)) {
            if (this.isBlockLocked(neighbor3) && this.getLockedBlock(neighbor3).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockUtils.isChest(neighbor4)) {
            return this.isBlockLocked(neighbor4) && this.getLockedBlock(neighbor4).getOwner() != player.getUniqueId();
        }

        return false;
    }

    public boolean isBlockUnderOrAboveNonOwnedLockedChest(OfflinePlayer player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());

        if (BlockUtils.isChest(neighbor1)) {
            if (this.isBlockLocked(neighbor1) && this.getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockUtils.isChest(neighbor2)) {
            return this.isBlockLocked(neighbor2) && this.getLockedBlock(neighbor2).getOwner() != player.getUniqueId();
        }

        return false;
    }
}