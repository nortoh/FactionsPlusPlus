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
import factionsplusplus.constants.GroupRole;
import factionsplusplus.data.daos.DefaultConfigurationFlagDao;
import factionsplusplus.data.daos.FactionDao;
import factionsplusplus.data.repositories.*;
import factionsplusplus.models.Faction;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.models.Poll;
import factionsplusplus.models.PollOption;
import factionsplusplus.models.World;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Command;
import factionsplusplus.models.ConfigOption;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.Gate;
import factionsplusplus.models.GroupMember;
import factionsplusplus.utils.BlockUtils;


@Singleton
public class DataService {
    private final DataProviderService dataProviderService;
    private final FactionRepository factionRepository;
    private final ClaimedChunkRepository claimedChunkRepository;
    private final LockedBlockRepository lockedBlockRepository;
    private final PlayerRecordRepository playerRecordRepository;
    private final CommandRepository commandRepository;
    private final ConfigOptionRepository configOptionRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
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
        PlayerRecordRepository playerRecordRepository,
        CommandRepository commandRepository,
        ConfigOptionRepository configOptionRepository,
        WarRepository warRepository,
        WorldRepository worldRepository,
        ConfigService configService,
        GateRepository gateRepository,
        PollRepository pollRepository,
        PollOptionRepository pollOptionRepository
    ) throws SQLException {
        this.dataProviderService = dataProviderService;
        this.factionRepository = factionRepository;
        this.claimedChunkRepository = claimedChunkRepository;
        this.lockedBlockRepository = lockedBlockRepository;
        this.playerRecordRepository = playerRecordRepository;
        this.commandRepository = commandRepository;
        this.configOptionRepository = configOptionRepository;
        this.warRepository = warRepository;
        this.worldRepository = worldRepository;
        this.configService = configService;
        this.gateRepository = gateRepository;
        this.pollRepository = pollRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.persistentData = this.dataProviderService.getPersistentData();
        this.initializePersistentData();
    }

    public void initializePersistentData() throws SQLException {
        this.persistentData.useHandle(handle -> {
            handle.execute("""
                CREATE TABLE IF NOT EXISTS default_flags (
                    name CHAR(255),
                    description TEXT,
                    type TINYINT,
                    expected_data_type VARCHAR(255),
                    default_value VARCHAR(255),
                    PRIMARY KEY (name)
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS factions (
                    id BINARY(16) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    description VARCHAR(255) DEFAULT NULL,
                    prefix VARCHAR(255),
                    bonus_power DOUBLE,
                    should_autoclaim BOOLEAN NOT NULL DEFAULT 0,
                    PRIMARY KEY (id),
                    UNIQUE KEY UNIQUE_NAME (name)
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS worlds (
                    id BINARY(16) NOT NULL,
                    PRIMARY KEY (id)
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS faction_bases (
                    id BINARY(16) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    faction_id BINARY(16) NOT NULL,
                    world_id BINARY(16) NOT NULL,
                    x_position DOUBLE NOT NULL,
                    y_position DOUBLE NOT NULL,
                    z_position DOUBLE NOT NULL,
                    allow_all_members BOOLEAN NOT NULL DEFAULT 0,
                    allow_allies BOOLEAN NOT NULL DEFAULT 0,
                    is_faction_default BOOLEAN NOT NULL DEFAULT 0,
                    PRIMARY KEY(id),
                    UNIQUE KEY FB_UNIQUE_LOCATION (world_id, x_position, y_position, z_position),
                    UNIQUE KEY FB_UNIQUE_NAME (name, faction_id),
                    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE,
                    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS faction_flags (
                    faction_id BINARY(16) NOT NULL,
                    flag_name CHAR(255) NOT NULL,
                    `value` VARCHAR(255),
                    PRIMARY KEY(faction_id, flag_name),
                    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE,
                    FOREIGN KEY(flag_name) REFERENCES default_flags(name) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS world_flags (
                    world_id BINARY(16) NOT NULL,
                    flag_name CHAR(255) NOT NULL,
                    `value` VARCHAR(255),
                    PRIMARY KEY(world_id, flag_name),
                    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE,
                    FOREIGN KEY(flag_name) REFERENCES default_flags(name) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                 CREATE TABLE IF NOT EXISTS players (
                    id BINARY(16) NOT NULL,
                    power DOUBLE NOT NULL DEFAULT 0,
                    is_admin_bypassing BOOLEAN NOT NULL DEFAULT 0,
                    login_count INTEGER NOT NULL DEFAULT 1,
                    last_logout DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    offline_power_lost DOUBLE NOT NULL DEFAULT 0,
                    PRIMARY KEY (id)
                 )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS faction_members (
                    faction_id BINARY(16) NOT NULL,
                    player_id BINARY(16) NOT NULL,
                    role INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY(faction_id, player_id),
                    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE,
                    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS claimed_chunks (
                    faction_id BINARY(16) NOT NULL,
                    world_id BINARY(16) NOT NULL,
                    x_position INTEGER NOT NULL,
                    z_position INTEGER NOT NULL,
                    PRIMARY KEY(world_id, x_position, z_position),
                    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE,
                    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS faction_wars (
                    id BINARY(16) NOT NULL,
                    attacker_id BINARY(16) NOT NULL,
                    defender_id BINARY(16) NOT NULL,
                    reason VARCHAR(1024),
                    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    ended_at DATETIME,
                    is_active BOOLEAN NOT NULL DEFAULT 1,
                    PRIMARY KEY(id),
                    FOREIGN KEY(attacker_id) REFERENCES factions(id) ON DELETE CASCADE,
                    FOREIGN KEY(defender_id) REFERENCES factions(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS locked_blocks (
                    id BINARY(16) NOT NULL,
                    world_id BINARY(16) NOT NULL,
                    faction_id BINARY(16) NOT NULL,
                    x_position INTEGER NOT NULL,
                    y_position INTEGER NOT NULL,
                    z_position INTEGER NOT NULL,
                    player_id BINARY(16) NOT NULL,
                    allow_allies BOOLEAN NOT NULL DEFAULT 0,
                    allow_faction_members BOOLEAN NOT NULL DEFAULT 0,
                    PRIMARY KEY(id),
                    UNIQUE KEY UNIQUE_POSITION (world_id, x_position, y_position, z_position),
                    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE,
                    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE,
                    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS locked_block_access_list (
                    locked_block_id BINARY(16) NOT NULL,
                    player_id BINARY(16) NOT NULL,
                    PRIMARY KEY (locked_block_id, player_id),
                    FOREIGN KEY(locked_block_id) REFERENCES locked_blocks(id) ON DELETE CASCADE,
                    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS faction_gates (
                    id BINARY(16) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    faction_id BINARY(16) NOT NULL,
                    material CHAR(255) NOT NULL,
                    world_id BINARY(16) NOT NULL,
                    position_one_location JSON,
                    position_two_location JSON,
                    trigger_location JSON,
                    is_vertical BOOLEAN NOT NULL DEFAULT 0,
                    is_open BOOLEAN NOT NULL DEFAULT 0,
                    PRIMARY KEY(id),
                    FOREIGN KEY(world_id) REFERENCES worlds(id) ON DELETE CASCADE,
                    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS faction_invites (
                    player_id BINARY(16) NOT NULL,
                    faction_id BINARY(16) NOT NULL,
                    invited_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY(player_id, faction_id),
                    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE,
                    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS faction_relations (
                    source_faction BINARY(16) NOT NULL,
                    target_faction BINARY(16) NOT NULL,
                    type TINYINT NOT NULL,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY(source_faction, target_faction),
                    FOREIGN KEY(source_faction) REFERENCES factions(id) ON DELETE CASCADE,
                    FOREIGN KEY(target_faction) REFERENCES factions(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS faction_laws (
                    id BINARY(16) NOT NULL,
                    faction_id BINARY(16) NOT NULL,
                    text TEXT NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS polls (
                    id BINARY(16) NOT NULL,
                    faction_id BINARY(16) NOT NULL,
                    question TEXT NOT NULL,
                    choices_allowed TINYINT NOT NULL DEFAULT 1,
                    created_at BINARY(16),
                    PRIMARY KEY(id),
                    FOREIGN KEY(faction_id) REFERENCES factions(id) ON DELETE CASCADE,
                    FOREIGN KEY(created_by) REFERENCES players(id) ON DELETE SET NULL
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS poll_options (
                    id UNSIGNED INT NOT NULL AUTO_INCREMENT,
                    poll_id BINARY(16) NOT NULL,
                    text TEXT NOT NULL,
                    PRIMARY KEY(id),
                    FORIEGN KEY(poll_id) REFERENCES polls(id) ON DELETE CASCADE
                )
            """);
            handle.execute("""
                CREATE TABLE IF NOT EXISTS poll_votes (
                    id BINARY(16) NOT NULL,
                    player_id BINARY(16) NOT NULL,
                    poll_id BINARY(15) NOT NULL,
                    option_id UNSIGNED INT NOT NULL,
                    PRIMARY KEY(player_id, poll_id, option_id),
                    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE SET NULL,
                    FOREIGN KEY(option_id) REFERENCES poll_options(id) ON DELETE CASCADE
                )
            """);
        });
    }

    /*
     * Saves all data in memory to disk. In most cases, data is written immediately. Data that is updated often may be saved using this function.
     */
    public void save() {
        this.playerRecordRepository.persist(); // save player stats
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
        this.playerRecordRepository.load();
        this.lockedBlockRepository.load();
        this.gateRepository.load();
        this.warRepository.load();
        this.pollRepository.load();
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
        this.addDefaultConfigurationFlag(new ConfigurationFlag("mustBeOfficerToManageLand", FlagDataType.Boolean, true), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("mustBeOfficerToInviteOthers", FlagDataType.Boolean, true), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("alliesCanInteractWithLand", FlagDataType.Boolean, this.configService.getBoolean("allowAllyInteraction")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("vassalageTreeCanInteractWithLand", FlagDataType.Boolean, this.configService.getBoolean("allowVassalageTreeInteraction")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("neutral", FlagDataType.Boolean, false), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("dynmapTerritoryColor", FlagDataType.Color, "#ff0000"), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("territoryAlertColor", FlagDataType.Color, this.configService.getString("territoryAlertColor")), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("prefixColor", FlagDataType.Color, "white"), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("allowFriendlyFire", FlagDataType.Boolean, false), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("acceptBonusPower", FlagDataType.Boolean, true), FlagType.Faction);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("enabledMobProtection", FlagDataType.Boolean, true), FlagType.Faction);

        // Worlds
        this.addDefaultConfigurationFlag(new ConfigurationFlag("enabled", FlagDataType.Boolean, true), FlagType.World);
        this.addDefaultConfigurationFlag(new ConfigurationFlag("allowClaims", FlagDataType.Boolean, true), FlagType.World);
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

    public void updatePlayersFactionRole(Faction faction, OfflinePlayer player, GroupRole role) {
        GroupMember member = faction.getMember(player.getUniqueId());
        if (member == null) faction.addMember(player.getUniqueId());
        faction.setMemberRole(player.getUniqueId(), role);
        this.persistentData.useExtension(FactionDao.class, dao -> {
            GroupMember factionMember = faction.getMember(player.getUniqueId());
            dao.upsert(faction.getUUID(), factionMember.getUUID(), factionMember.getRole());
        });
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

    public void createPlayerRecord(PlayerRecord record) {
        this.playerRecordRepository.create(record);
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
        return this.playerRecordRepository.all().values();
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

    public GateRepository getGateRepository() {
        return this.gateRepository;
    }

    public PollRepository getPollRepository() {
        return this.pollRepository;
    }

    public Poll getPoll(UUID uuid) {
        return this.pollRepository.get(uuid);
    }

    public PollOptionRepository getPollOptionRepository() {
        return this.pollOptionRepository;
    }

    public PollOption getPollOption(UUID uuid) {
        return this.pollOptionRepository.get(uuid);
    }
}