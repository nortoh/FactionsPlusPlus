/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.events.FactionClaimEvent;
import dansplugins.factionsystem.events.FactionUnclaimEvent;
import dansplugins.factionsystem.models.ClaimedChunk;
import dansplugins.factionsystem.models.LockedBlock;
import dansplugins.factionsystem.models.PlayerRecord;
import dansplugins.factionsystem.objects.domain.Duel;
import dansplugins.factionsystem.objects.domain.War;
import dansplugins.factionsystem.repositories.ClaimedChunkRepository;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.repositories.LockedBlockRepository;
import dansplugins.factionsystem.repositories.PlayerRecordRepository;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.Gate;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.DynmapIntegrationService;
import dansplugins.factionsystem.services.FactionService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.InteractionAccessChecker;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.extended.BlockChecker;
import dansplugins.factionsystem.utils.extended.Messenger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;
import static org.bukkit.Material.LADDER;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class PersistentData {
    final HashSet<War> wars = new HashSet<>();
    private final LocaleService localeService;
    private final ConfigService configService;
    private final MedievalFactions medievalFactions;
    private final PlayerService playerService;
    private final MessageService messageService;
    private final Messenger messenger;
    private final EphemeralData ephemeralData;
    private final Logger logger;
    private final FactionRepository factionRepository;
    private final ClaimedChunkRepository claimedChunkRepository;
    private final LockedBlockRepository lockedBlockRepository;
    private final PlayerRecordRepository playerRecordRepository;
    private final InteractionAccessChecker interactionAccessChecker;
    private final ArrayList<LockedBlock> lockedBlocks = new ArrayList<>();
    private final ChunkDataAccessor chunkDataAccessor = new ChunkDataAccessor();
    private final LocalStorageService localStorageService = new LocalStorageService(this);

    private final DynmapIntegrationService dynmapService;
    private final BlockChecker blockChecker;

    private final FactionService factionService;

    @Inject
    public PersistentData(
        LocaleService localeService,
        ConfigService configService,
        MedievalFactions medievalFactions,
        PlayerService playerService,
        MessageService messageService,
        Messenger messenger,
        Logger logger,
        EphemeralData ephemeralData,
        BlockChecker blockChecker,
        DynmapIntegrationService dynmapService,
        InteractionAccessChecker interactionAccessChecker,
        FactionRepository factionRepository,
        ClaimedChunkRepository claimedChunkRepository,
        LockedBlockRepository lockedBlockRepository,
        PlayerRecordRepository playerRecordRepository,
        FactionService factionService
    ) {
        this.localeService = localeService;
        this.configService = configService;
        this.medievalFactions = medievalFactions;
        this.playerService = playerService;
        this.messageService = messageService;
        this.messenger = messenger;
        this.ephemeralData = ephemeralData;
        this.logger = logger;
        this.dynmapService = dynmapService;
        this.interactionAccessChecker = interactionAccessChecker;
        this.blockChecker = blockChecker;
        this.factionRepository = factionRepository;
        this.claimedChunkRepository = claimedChunkRepository;
        this.lockedBlockRepository = lockedBlockRepository;
        this.playerRecordRepository = playerRecordRepository;
        this.factionService = factionService;
    }

    public BlockChecker getBlockChecker() {
        return blockChecker;
    }

    public ChunkDataAccessor getChunkDataAccessor() {
        return chunkDataAccessor;
    }

    public LocalStorageService getLocalStorageService() {
        return localStorageService;
    }

    /**
     * Method to get a Faction by its name.
     * <p>
     * This method utilises {@link #getFaction(String, boolean, boolean)} to obtain the Faction with the given name.
     * </p>
     *
     * @param name of the Faction desired (Can be {@code null}).
     * @return {@link Faction} or {@code null}.
     * @see #getFaction(String, boolean, boolean)
     */
    public Faction getFaction(String name) {
        return this.factionRepository.get(name);
    }

    /**
     * Method to get a Faction by its prefix.
     * <p>
     * This method utilises {@link #getFaction(String, boolean, boolean)} to obtain the Faction with the given prefix.
     * </p>
     *
     * @param prefix of the Faction desired (Can be {@code null}).
     * @return {@link Faction} or {@code null}.
     * @see #getFaction(String, boolean, boolean)
     */
    public Faction getFactionByPrefix(String prefix) {
        return getFaction(prefix, true, true);
    }

    /**
     * Method to obtain a Faction from the given string.
     * <p>
     * This method can check Faction name and/or Faction prefix depending on the parameters specified.
     * <br>If you wish to only check prefix, provide the string and make sure both booleans are {@code true}.
     * <br>If you wish to only check name, provide the string and make sure both booleans are {@code false}.
     * <br>If you wish to check everything, provide the string and make sure the first boolean is {@code true} only.
     * </p>
     *
     * @param text            which you'd like to obtain the Faction from.
     * @param checkPrefix     a toggle for checking prefix.
     * @param onlyCheckPrefix a toggle for only checking prefix.
     * @return {@link Faction} or {@code null}.
     * @see #getFaction(String)
     * @see #getFactionByPrefix(String)
     */
    public Faction getFaction(String text, boolean checkPrefix, boolean onlyCheckPrefix) {
        return this.factionRepository.get(text);
    }

    public Faction getPlayersFaction(UUID playerUUID) {
        for (Faction faction : this.factionRepository.all()) {
            if (faction.isMember(playerUUID)) {
                return faction;
            }
        }
        return null;
    }

    public PlayerRecord getPlayerRecord(UUID playerUUID) {
        return this.playerRecordRepository.get(playerUUID);
    }

    public LockedBlock getLockedBlock(Block block) {
        return getLockedBlock(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

    private LockedBlock getLockedBlock(int x, int y, int z, String world) {
        return this.lockedBlockRepository.get(x, y, z, world);
    }

    public ArrayList<Faction> getFactionsInVassalageTree(Faction initialFaction) {
        ArrayList<Faction> foundFactions = new ArrayList<>();

        foundFactions.add(initialFaction);

        boolean newFactionsFound = true;

        int numFactionsFound;

        while (newFactionsFound) {
            ArrayList<Faction> toAdd = new ArrayList<>();
            for (Faction current : foundFactions) {

                // record number of factions
                numFactionsFound = foundFactions.size();

                Faction liege = this.factionRepository.get(current.getLiege());
                if (liege != null) {
                    if (!containsFactionByName(toAdd, liege) && !containsFactionByName(foundFactions, liege)) {
                        toAdd.add(liege);
                        numFactionsFound++;
                    }

                    for (String vassalName : liege.getVassals()) {
                        Faction vassal = this.factionRepository.get(vassalName);
                        if (!containsFactionByName(toAdd, vassal) && !containsFactionByName(foundFactions, vassal)) {
                            toAdd.add(vassal);
                            numFactionsFound++;
                        }
                    }
                }

                for (String vassalName : current.getVassals()) {
                    Faction vassal = this.factionRepository.get(vassalName);
                    if (!containsFactionByName(toAdd, vassal) && !containsFactionByName(foundFactions, vassal)) {
                        toAdd.add(vassal);
                        numFactionsFound++;
                    }
                }
                // if number of factions not different then break loop
                if (numFactionsFound == foundFactions.size()) {
                    newFactionsFound = false;
                }
            }
            foundFactions.addAll(toAdd);
            toAdd.clear();
        }
        return foundFactions;
    }

    private boolean containsFactionByName(ArrayList<Faction> list, Faction faction) {
        for (Faction f : list) {
            if (f.getName().equalsIgnoreCase(faction.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isInFaction(UUID playerUUID) {
        for (Faction faction : this.factionRepository.all()) {
            if (faction.isMember(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlockLocked(Block block) {
        return isBlockLocked(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

    private boolean isBlockLocked(int x, int y, int z, String world) {
        LockedBlock block = this.lockedBlockRepository.get(x, y, z, world);
        if (block != null) {
            return true;
        }
        return false;
    }

    public boolean isGateBlock(Block targetBlock) {
        for (Faction faction : this.factionRepository.all()) {
            for (Gate gate : faction.getGates()) {
                if (gate.hasBlock(targetBlock)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPlayerInFactionInVassalageTree(Player player, Faction faction) {
        ArrayList<Faction> factionsToCheck = getFactionsInVassalageTree(faction);
        for (Faction f : factionsToCheck) {
            if (f.isMember(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public void removeAllLocks(String factionName) {
        Iterator<LockedBlock> itr = this.lockedBlockRepository.all().iterator();

        while (itr.hasNext()) {
            LockedBlock currentBlock = itr.next();
            if (currentBlock.getFactionName().equalsIgnoreCase(factionName)) {
                try {
                    itr.remove();
                } catch (Exception e) {
                    System.out.println("An error has occurred during lock removal.");
                }
            }
        }
    }

    public void createActivityRecordForEveryOfflinePlayer() { // this method is to ensure that when updating to a version with power decay, even players who never log in again will experience power decay
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerRecord record = this.getPlayerRecord(player.getUniqueId());
            if (record == null) {
                PlayerRecord newRecord = new PlayerRecord(player.getUniqueId(), 1);
                newRecord.setLastLogout(ZonedDateTime.now());
                this.playerRecordRepository.create(newRecord);
            }
        }
    }

    // TODO: implement this in FactionService
    public Faction getRandomFaction() {
        /*Random generator = new Random();
        int randomIndex = generator.nextInt(factions.size());
        return factions.get(randomIndex);*/
        return null;
    }

    public void addWar(War war) {
        wars.add(war);
    }

    public void addFaction(Faction faction) {
        //factions.add(faction);
    }

    public int getFactionIndexOf(Faction faction) {
        //return factions.indexOf(faction);
        return 0;
    }

    public Faction getFactionByIndex(int i) {
        //return factions.get(i);
        return null;
    }

    public void removeFactionByIndex(int i) {
        //factions.remove(i);
    }

    public void removePoliticalTiesToFaction(String factionName) {
        for (Faction faction : this.factionRepository.all()) {

            // remove records of alliances/wars associated with this faction
            if (faction.isAlly(factionName)) {
                faction.removeAlly(factionName);
            }
            if (faction.isEnemy(factionName)) {
                faction.removeEnemy(factionName);
            }

            // remove liege and vassal references associated with this faction
            if (faction.isLiege(factionName)) {
                faction.setLiege("none");
            }

            if (faction.isVassal(factionName)) {
                faction.removeVassal(factionName);
            }
        }
    }

    public List<ClaimedChunk> getChunksClaimedByFaction(String factionName) {
        List<ClaimedChunk> output = new ArrayList<>();
        for (ClaimedChunk chunk : this.claimedChunkRepository.all()) {
            if (chunk.getHolder().equalsIgnoreCase(factionName)) {
                output.add(chunk);
            }
        }
        return output;
    }

    public void addPlayerRecord(PlayerRecord record) {
        if (this.hasPlayerRecord(record.getPlayerUUID())) {
            return;
        }
        this.playerRecordRepository.create(record);
    }

    public boolean hasPlayerRecord(UUID playerUUID) {
        PlayerRecord record = this.playerRecordRepository.get(playerUUID);
        if (record != null) {
            return true;
        }
        return false;
    }

    public int getNumFactions() {
        return this.factionRepository.all().size();
    }

    public int getNumPlayers() {
        return this.playerRecordRepository.all().size();
    }

    public void updateFactionReferencesDueToNameChange(String oldName, String newName) {
        // Change Ally/Enemy/Vassal/Liege references
        // TODO: reimplement, we should consider assigning UUIDs to factions that don't change to avoid this...
        //this.factionRepository.all().forEach(fac -> fac.updateData(oldName, newName));

        // Change Claims
        this.claimedChunkRepository.all().stream().filter(cc -> cc.getHolder().equalsIgnoreCase(oldName))
                .forEach(cc -> cc.setHolder(newName));

        // Locked Blocks
        this.lockedBlockRepository.all().stream().filter(lb -> lb.getFactionName().equalsIgnoreCase(oldName))
                .forEach(lb -> lb.setFaction(newName));
    }

    public long removeLiegeAndVassalReferencesToFaction(String factionName) {
        long changes = this.factionRepository.all().stream()
                .filter(f -> f.isLiege(factionName) || f.isVassal(factionName))
                .count(); // Count changes

        this.factionRepository.all().stream().filter(f -> f.isLiege(factionName)).forEach(f -> f.setLiege("none"));
        this.factionRepository.all().stream().filter(f -> f.isVassal(factionName)).forEach(Faction::clearVassals);

        return changes;
    }

    public boolean isBlockInGate(Block block, Player player) {
        for (Faction faction : this.factionRepository.all()) {
            for (Gate gate : faction.getGates()) {
                if (gate.hasBlock(block)) {
                    playerService.sendMessage(player, ChatColor.RED + String.format(localeService.get("BlockIsPartOfGateMustRemoveGate"), gate.getName())
                            , Objects.requireNonNull(messageService.getLanguage().getString("BlockIsPartOfGateMustRemoveGate")).replace("#name#", gate.getName()), true);
                    return true;
                }
            }
        }
        return false;
    }

    public void addLockedBlock(LockedBlock newLockedBlock) {
        this.lockedBlockRepository.create(newLockedBlock);
    }

    public void resetPowerLevels() {
        final int initialPowerLevel = this.configService.getInt("initialPowerLevel");
        this.playerRecordRepository.all().forEach(record -> record.setPower(initialPowerLevel));
    }

    public void initiatePowerIncreaseForAllPlayers() {
        for (PlayerRecord record : this.playerRecordRepository.all()) {
            try {
                initiatePowerIncrease(record);
            } catch (Exception ignored) {

            }
        }
    }

    private void initiatePowerIncrease(PlayerRecord record) {
        // TODO: reimplement
        /*
        if (powerRecord.getPower() < powerRecord.maxPower() && Objects.requireNonNull(getServer().getPlayer(powerRecord.getPlayerUUID())).isOnline()) {
            powerRecord.increasePower();
            playerService.sendMessage(getServer().getPlayer(powerRecord.getPlayerUUID()), ChatColor.GREEN + String.format(localeService.get("AlertPowerLevelIncreasedBy"), configService.getInt("powerIncreaseAmount"))
                    , Objects.requireNonNull(messageService.getLanguage().getString("AlertPowerLevelIncreasedBy"))
                            .replace("#amount#", String.valueOf(configService.getInt("powerIncreaseAmount"))), true);
        }*/
    }

    public void disbandAllZeroPowerFactions() {
        ArrayList<String> factionsToDisband = new ArrayList<>();
        for (Faction faction : this.factionRepository.all()) {
            if (this.factionService.getCumulativePowerLevel(faction) == 0) {
                factionsToDisband.add(faction.getName());
            }
        }

        for (String factionName : factionsToDisband) {
            messenger.sendAllPlayersInFactionMessage(this.factionRepository.get(factionName), playerService.decideWhichMessageToUse(ChatColor.RED + localeService.get("AlertDisbandmentDueToZeroPower"), messageService.getLanguage().getString("AlertDisbandmentDueToZeroPower")));
            removeFaction(factionName);
            System.out.printf((localeService.get("DisbandmentDueToZeroPower")) + "%n", factionName);
        }
    }

    private void removeFaction(String name) {

        Faction factionToRemove = this.factionRepository.get(name);

        if (factionToRemove != null) {
            // remove claimed land objects associated with this faction
            getChunkDataAccessor().removeAllClaimedChunks(factionToRemove.getName());
            this.dynmapService.updateClaimsIfAble();

            // remove locks associated with this faction
            removeAllLocks(factionToRemove.getName());

            removePoliticalTiesToFaction(factionToRemove.getName());

            int index = -1;
            for (int i = 0; i < getNumFactions(); i++) {
                if (getFactionByIndex(i).getName().equalsIgnoreCase(name)) {
                    index = i;
                }
            }
            if (index != -1) {
                removeFactionByIndex(index);
            }
        }
    }

    public void decreasePowerForInactivePlayers() {
        for (PlayerRecord record : this.playerRecordRepository.all()) {
            Player player = getServer().getPlayer(record.getPlayerUUID());
            boolean isOnline = false;
            if (player != null) {
                isOnline = player.isOnline();
            }
            if (!isOnline && this.configService.getBoolean("powerDecreases") && record.getMinutesSinceLastLogout() > configService.getInt("minutesBeforePowerDecrease")) {
                record.incrementPowerLost();
                record.decreasePower();
            }
        }
    }

    public List<SortableFaction> getSortedListOfFactions() {
        return this.factionRepository.all().stream()
                .map(fac -> new SortableFaction(fac, this.factionService.getCumulativePowerLevel(fac)))
                .sorted() // Sort the Factions by Power.
                .collect(Collectors.toList());
    }

    public Gate getGate(Block targetBlock) {
        return this.factionRepository.all().stream().flatMap(faction -> faction.getGates().stream())
                .filter(gate -> gate.hasBlock(targetBlock)).findFirst().orElse(null);
    }

    public Faction getGateFaction(Gate gate) {
        return this.factionRepository.all().stream()
                .filter(faction -> faction.getGates().contains(gate)).findFirst().orElse(null);
    }

    public void removeLockedBlock(Block block) {
        LockedBlock b = this.lockedBlockRepository.get(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
        if (b != null) {
            this.lockedBlockRepository.delete(b);
        }
    }

    public boolean isPrefixTaken(String newPrefix) {
        return this.factionRepository.all().stream().map(Faction::getPrefix).anyMatch(prefix -> prefix.equalsIgnoreCase(newPrefix));
    }

    public ArrayList<Faction> getFactions() {
        return this.factionRepository.all();
    }

    public ArrayList<PlayerRecord> getPlayerRecords() {
        return this.playerRecordRepository.all();
    }

    public InteractionAccessChecker getInteractionAccessChecker() {
        return interactionAccessChecker;
    }

    public static class SortableFaction implements Comparable<SortableFaction> {
        private final Faction faction;
        private final int power;

        public SortableFaction(Faction faction, int cumulativePower) {
            this.faction = faction;
            this.power = cumulativePower;
        }

        public Faction getFaction() {
            return faction;
        }

        public int getPower() {
            return power;
        }

        @Override
        public int compareTo(SortableFaction o) {
            int comparison = Integer.compare(getPower(), o.getPower()); // Current > Greater (higher first)

            // return the opposite of the result of the comparison so that factions will be sorted from highest to lowest power
            return Integer.compare(0, comparison);
        }

    }

    /**
     * This class assists in the management of claimed chunks.
     *
     * @author Daniel McCoy Stephenson
     */
    public class ChunkDataAccessor {

        /**
         * This is the method that can be utilized to access the singleton instance of the Local Chunk Service.
         *
         * @return The singleton instance of the Local Chunk Service.
         */
        public ChunkDataAccessor getInstance() {
            return chunkDataAccessor;
        }

        /**
         * This public method can be used to retrieve a claimed chunk. A returned value of null means the chunk is not claimed.
         *
         * @param chunk The chunk to grab.
         * @return The associated claimed chunk.
         */
        public ClaimedChunk getClaimedChunk(Chunk chunk) {
            return getClaimedChunk(chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
        }

        /**
         * This method can be used to claim a radius of chunks around a player.
         *
         * @param depth            The radius of chunks to claim.
         * @param claimant         The player claiming the chunks.
         * @param location         The central location of claiming.
         * @param claimantsFaction The claimant's faction.
         */
        public void radiusClaimAtLocation(int depth, Player claimant, Location location, Faction claimantsFaction) {
            int maxClaimRadius = configService.getInt("maxClaimRadius");

            // check if depth is valid
            if (depth < 0 || depth > maxClaimRadius) {
                playerService.sendMessage(claimant, ChatColor.RED + String.format(localeService.get("RadiusRequirement"), maxClaimRadius),
                        Objects.requireNonNull(messageService.getLanguage().getString("RadiusRequirement"))
                                .replace("#number#", String.valueOf(maxClaimRadius)), true);
                return;
            }

            // if depth is 0, we just need to claim the chunk the player is on
            if (depth == 0) {
                claimChunkAtLocation(claimant, location, claimantsFaction);
                return;
            }

            // claim chunks
            final Chunk initial = location.getChunk();
            final Set<Chunk> chunkSet = obtainChunks(initial, depth);
            chunkSet.forEach(chunk -> claimChunkAtLocation(
                    claimant, getChunkCoords(chunk), chunk.getWorld(), claimantsFaction
            ));
        }

        /**
         * This method can be used to unclaim a radius of chunks around a player.
         *
         * @param radius  The radius of chunks to unclaim.
         * @param player  The player unclaiming the chunks.
         * @param faction The player's faction.
         */
        public void radiusUnclaimAtLocation(int radius, Player player, Faction faction) {
            final int maxChunksUnclaimable = 999;

            // check if radius is valid
            if (radius <= 0 || radius > maxChunksUnclaimable) {
                player.sendMessage(ChatColor.RED + String.format(localeService.getText("RadiusRequirement"), maxChunksUnclaimable));
                return;
            }

            // unclaim chunks
            final Set<Chunk> chunkSet = obtainChunks(player.getLocation().getChunk(), radius);
            chunkSet.stream()
                    .map(c -> isChunkClaimed(c.getX(), c.getZ(), c.getWorld().getName()))
                    .filter(Objects::nonNull)
                    .forEach(chunk -> removeChunk(chunk, player, faction));
        }

        /**
         * Claims a singular chunk at a location.
         *
         * @param claimant         The player claiming the chunk.
         * @param location         The location getting claimed.
         * @param claimantsFaction The player's faction.
         */
        public void claimChunkAtLocation(Player claimant, Location location, Faction claimantsFaction) {
            double[] chunkCoords = getChunkCoords(location);
            claimChunkAtLocation(claimant, chunkCoords, location.getWorld(), claimantsFaction);
        }

        /**
         * Unclaims a chunk at a location.
         *
         * @param player         The player unclaiming the chunk.
         * @param playersFaction The player's faction.
         */
        public void removeChunkAtPlayerLocation(Player player, Faction playersFaction) {
            // get player coordinates
            double[] playerCoords = new double[2];
            playerCoords[0] = player.getLocation().getChunk().getX();
            playerCoords[1] = player.getLocation().getChunk().getZ();

            // handle admin bypass
            if (ephemeralData.getAdminsBypassingProtections().contains(player.getUniqueId())) {
                ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], Objects.requireNonNull(player.getLocation().getWorld()).getName());
                if (chunk != null) {
                    removeChunk(chunk, player, getFaction(chunk.getHolder()));
                    playerService.sendMessage(player, ChatColor.GREEN + localeService.get("LandClaimedUsingAdminBypass")
                            , "LandClaimedUsingAdminBypass", false);
                    return;
                }
                playerService.sendMessage(player, ChatColor.RED + localeService.get("LandNotCurrentlyClaimed")
                        , "LandNotCurrentlyClaimed", false);
                return;
            }

            ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], Objects.requireNonNull(player.getLocation().getWorld()).getName());

            // ensure that chunk is claimed
            if (chunk == null) {
                return;
            }

            // ensure that the chunk is claimed by the player's faction.
            if (!chunk.getHolder().equalsIgnoreCase(playersFaction.getName())) {
                playerService.sendMessage(player, ChatColor.RED + String.format(localeService.get("LandClaimedBy"), chunk.getHolder())
                        , Objects.requireNonNull(messageService.getLanguage().getString("LandClaimedBy")).replace("#player#", chunk.getHolder()), true);
                return;
            }

            // initiate removal
            removeChunk(chunk, player, playersFaction);
            playerService.sendMessage(player, ChatColor.GREEN + localeService.get("LandUnclaimed"),
                    "LandUnclaimed", false);
        }

        /**
         * This can be used to check which faction has laid claim to a chunk.
         *
         * @param player The player whose location we will be checking.
         * @return The name of the faction that has claimed the chunk. A value of "unclaimed" will be returned if the chunk is unclaimed.
         */
        public String checkOwnershipAtPlayerLocation(Player player) {
            double[] playerCoords = new double[2];
            playerCoords[0] = player.getLocation().getChunk().getX();
            playerCoords[1] = player.getLocation().getChunk().getZ();
            ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], Objects.requireNonNull(player.getLocation().getWorld()).getName());
            if (chunk != null) {
                return chunk.getHolder();
            }
            return "unclaimed";
        }

        /**
         * Checks if a gate is in a chunk.
         *
         * @param gate  The gate to check.
         * @param chunk The claimed chunk to check.
         * @return Whether the gate is in the claimed chunk.
         */
        public boolean isGateInChunk(Gate gate, ClaimedChunk chunk) {
            return (gate.getTopLeftChunkX() == chunk.getCoordinates()[0] || gate.getBottomRightChunkX() == chunk.getCoordinates()[0])
                    && (gate.getTopLeftChunkZ() == chunk.getCoordinates()[1] || gate.getBottomRightChunkZ() == chunk.getCoordinates()[1]);
        }

        /**
         * This can be used to retrieve the number of chunks claimed by a faction.
         *
         * @param factionName The name of the faction we are checking.
         * @return An integer indicating how many chunks have been claimed by this faction.
         */
        public int getChunksClaimedByFaction(String factionName) {
            int counter = 0;
            for (ClaimedChunk chunk : claimedChunkRepository.all()) {
                if (chunk.getHolder().equalsIgnoreCase(factionName)) {
                    counter++;
                }
            }
            return counter;
        }

        /**
         * This can be used to check if a chunk is claimed.
         *
         * @param chunk The chunk we are checking.
         * @return A boolean indicating if the chunk is claimed.
         */
        public boolean isClaimed(Chunk chunk) {
            for (ClaimedChunk claimedChunk : claimedChunkRepository.all()) {
                if (claimedChunk.getCoordinates()[0] == chunk.getX() && claimedChunk.getCoordinates()[1] == chunk.getZ() && claimedChunk.getWorldName().equalsIgnoreCase(chunk.getWorld().getName())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * This can be used to unclaim every chunk that a faction owns.
         *
         * @param factionName The name of the faction we are removing all claimed chunks from.
         */
        public void removeAllClaimedChunks(String factionName) {
            Iterator<ClaimedChunk> itr = claimedChunkRepository.all().iterator();

            while (itr.hasNext()) {
                ClaimedChunk currentChunk = itr.next();
                if (currentChunk.getHolder().equalsIgnoreCase(factionName)) {
                    try {
                        itr.remove();
                    } catch (Exception e) {
                        System.out.println(localeService.get("ErrorClaimedChunkRemoval"));
                    }
                }
            }
        }

        /**
         * This can be used to check if a faction has more claimed land than power.
         *
         * @param faction The faction we are checking.
         * @return Whether the faction's claimed land exceeds their power.
         */
        public boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
            return (getChunksClaimedByFaction(faction.getName()) > factionService.getCumulativePowerLevel(faction));
        }

        /**
         * If a player is exceeding their demesne limit, this method will inform them.
         *
         * @param player The player to inform.
         */
        public void informPlayerIfTheirLandIsInDanger(Player player) {
            Faction faction = getPlayersFaction(player.getUniqueId());
            if (faction != null) {
                if (isFactionExceedingTheirDemesneLimit(faction)) {
                    playerService.sendMessage(player, ChatColor.RED + localeService.get("AlertMoreClaimedChunksThanPower")
                            , "AlertMoreClaimedChunksThanPower", false);
                }
            }
        }

        /**
         * This handles interaction within a claimed chunk for the PlayerInteractEvent event.
         *
         * @param event        The PlayerInteractEvent event.
         * @param claimedChunk The chunk that has been interacted with.
         */
        public void handleClaimedChunkInteraction(PlayerInteractEvent event, ClaimedChunk claimedChunk) {
            // player not in a faction and isn't overriding
            if (!isInFaction(event.getPlayer().getUniqueId()) && !ephemeralData.getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

                Block block = event.getClickedBlock();
                if (configService.getBoolean("nonMembersCanInteractWithDoors") && block != null && blockChecker.isDoor(block)) {
                    // allow non-faction members to interact with doors
                    return;
                }

                event.setCancelled(true);
            }

            // TODO: simplify this code with a call to the shouldEventBeCancelled() method in InteractionAccessChecker.java

            final Faction playersFaction = getPlayersFaction(event.getPlayer().getUniqueId());
            if (playersFaction == null) {
                return;
            }

            // if player's faction is not the same as the holder of the chunk and player isn't overriding
            if (!(playersFaction.getName().equalsIgnoreCase(claimedChunk.getHolder())) && !ephemeralData.getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

                Block block = event.getClickedBlock();
                if (configService.getBoolean("nonMembersCanInteractWithDoors") && block != null && blockChecker.isDoor(block)) {
                    // allow non-faction members to interact with doors
                    return;
                }

                // if enemy territory
                if (playersFaction.isEnemy(claimedChunk.getHolder())) {
                    // if not interacting with chest
                    if (canBlockBeInteractedWith(event)) {
                        // allow placing ladders
                        if (configService.getBoolean("laddersPlaceableInEnemyFactionTerritory")) {
                            if (event.getMaterial() == LADDER) {
                                return;
                            }
                        }
                        // allow eating
                        if (materialAllowed(event.getMaterial())) {
                            return;
                        }
                        // allow blocking
                        if (event.getPlayer().getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                            return;
                        }
                    }
                }

                if (!interactionAccessChecker.isOutsiderInteractionAllowed(event.getPlayer(), claimedChunk, playersFaction)) {
                    event.setCancelled(true);
                }
            }
        }

        /**
         * This can be used to forcefully claim a chunk at the players location, regardless of requirements.
         *
         * @param player  The player whose location we are using.
         * @param faction The faction we are claiming the chunk for.
         */
        public void forceClaimAtPlayerLocation(Player player, Faction faction) {
            Location location = player.getLocation();

            ClaimedChunk claimedChunk = getClaimedChunk(location.getChunk());

            if (claimedChunk != null) {
                removeChunk(claimedChunk, player, faction);
            }

            addClaimedChunk(location.getChunk(), faction, Objects.requireNonNull(location.getWorld()));
        }

        /**
         * This is a private method intended to be used by this class to retrieve a claimed chunk.
         *
         * @param x     The x coordinate of the chunk to retrieve.
         * @param z     The z coordinate of the chunk to retrieve.
         * @param world The world that the chunk to retrieve is in.
         * @return The claimed chunk at the given location. A value of null indicates that the chunk is not claimed.
         */
        private ClaimedChunk getClaimedChunk(int x, int z, String world) {
            return claimedChunkRepository.get(x, z, world);
        }

        private Set<Chunk> obtainChunks(Chunk initial, int radius) {
            final Set<Chunk> chunkSet = new HashSet<>(); // Avoid duplicates without checking for it yourself.
            for (int x = initial.getX() - radius; x <= initial.getX() + radius; x++) {
                for (int z = initial.getZ() - radius; z <= initial.getZ() + radius; z++) {
                    chunkSet.add(initial.getWorld().getChunkAt(x, z));
                }
            }
            return chunkSet;
        }

        private void claimChunkAtLocation(Player claimant, double[] chunkCoords, World world, Faction claimantsFaction) {

            // if demesne limit enabled
            if (configService.getBoolean("limitLand")) {
                // if at demesne limit
                if (!(getChunksClaimedByFaction(claimantsFaction.getName()) < factionService.getCumulativePowerLevel(claimantsFaction))) {
                    playerService.sendMessage(claimant, ChatColor.RED + localeService.get("AlertReachedDemesne")
                            , "AlertReachedDemesne", false);
                    return;
                }
            }

            // check if land is already claimed
            ClaimedChunk chunk = isChunkClaimed(chunkCoords[0], chunkCoords[1], world.getName());
            if (chunk != null) {
                // chunk already claimed
                Faction targetFaction = getFaction(chunk.getHolder());

                // if holder is player's faction
                if (targetFaction.getName().equalsIgnoreCase(claimantsFaction.getName()) && !claimantsFaction.getAutoClaimStatus()) {
                    playerService.sendMessage(claimant, ChatColor.RED + localeService.get("LandAlreadyClaimedByYourFaction")
                            , "LandAlreadyClaimedByYourFaction", false);
                    return;
                }

                // if not at war with target faction
                if (!claimantsFaction.isEnemy(targetFaction.getName())) {
                    playerService.sendMessage(claimant, ChatColor.RED + "You must be at war with a faction to conquer land from them."
                            , "IsNotEnemy", false);
                    return;
                }

                // surrounded chunk protection check
                if (configService.getBoolean("surroundedChunksProtected")) {
                    if (isClaimedChunkSurroundedByChunksClaimedBySameFaction(chunk)) {
                        playerService.sendMessage(claimant, ChatColor.RED + localeService.get("SurroundedChunkProtected"),
                                "SurroundedChunkProtected", false);
                        return;
                    }
                }

                int targetFactionsCumulativePowerLevel = factionService.getCumulativePowerLevel(targetFaction);
                int chunksClaimedByTargetFaction = getChunksClaimedByFaction(targetFaction.getName());

                // if target faction does not have more land than their demesne limit
                if (!(targetFactionsCumulativePowerLevel < chunksClaimedByTargetFaction)) {
                    playerService.sendMessage(claimant, ChatColor.RED + localeService.get("TargetFactionNotOverClaiming")
                            , "TargetFactionNotOverClaiming", false);
                    return;
                }

                // CONQUERABLE

                // remove locks on this chunk
                lockedBlockRepository.all().removeIf(block -> chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                        chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ());

                FactionClaimEvent claimEvent = new FactionClaimEvent(claimantsFaction, claimant, chunk.getChunk());
                Bukkit.getPluginManager().callEvent(claimEvent);
                if (!claimEvent.isCancelled()) {
                    claimedChunkRepository.delete(chunk);

                    Chunk toClaim = world.getChunkAt((int) chunkCoords[0], (int) chunkCoords[1]);
                    addClaimedChunk(toClaim, claimantsFaction, claimant.getWorld());
                    playerService.sendMessage(claimant, ChatColor.GREEN + String.format(localeService.get("AlertLandConqueredFromAnotherFaction"), targetFaction.getName(), getChunksClaimedByFaction(claimantsFaction.getName()), factionService.getCumulativePowerLevel(claimantsFaction))
                            , Objects.requireNonNull(messageService.getLanguage().getString("AlertLandConqueredFromAnotherFaction")).replace("#name", targetFaction.getName()).replace("#number#", String.valueOf(getChunksClaimedByFaction(claimantsFaction.getName()))).replace("#max#", String.valueOf(factionService.getCumulativePowerLevel(claimantsFaction))), true);

                    messenger.sendAllPlayersInFactionMessage(targetFaction, playerService
                            .decideWhichMessageToUse(ChatColor.RED + String.format(localeService.get("AlertLandConqueredFromYourFaction"), claimantsFaction.getName())
                                    , Objects.requireNonNull(messageService.getLanguage().getString("AlertLandConqueredFromYourFaction")).replace("#number#", claimantsFaction.getName())));
                }
            } else {
                Chunk toClaim = world.getChunkAt((int) chunkCoords[0], (int) chunkCoords[1]);
                FactionClaimEvent claimEvent = new FactionClaimEvent(claimantsFaction, claimant, toClaim);
                Bukkit.getPluginManager().callEvent(claimEvent);
                if (!claimEvent.isCancelled()) {
                    // chunk not already claimed
                    addClaimedChunk(toClaim, claimantsFaction, claimant.getWorld());
                    playerService.sendMessage(claimant, ChatColor.GREEN + String.format(localeService.get("AlertLandClaimed"), getChunksClaimedByFaction(claimantsFaction.getName()), factionService.getCumulativePowerLevel(claimantsFaction))
                            , Objects.requireNonNull(messageService.getLanguage().getString("AlertLandClaimed")).replace("#number#", String.valueOf(getChunksClaimedByFaction(claimantsFaction.getName()))).replace("#max#", String.valueOf(factionService.getCumulativePowerLevel(claimantsFaction))), true);
                }
            }
        }

        /**
         * Adds a claimed chunk to persistent data.
         *
         * @param chunk   The chunk we will be creating a new claimed chunk with.
         * @param faction The faction that will own the claimed chunk.
         * @param world   The world that the claimed chunk is located in.
         */
        private void addClaimedChunk(Chunk chunk, Faction faction, World world) {
            ClaimedChunk newChunk = new ClaimedChunk(chunk);
            newChunk.setHolder(faction.getName());
            claimedChunkRepository.create(newChunk);            
        }

        /**
         * This can be used to retrieve the x and z coordinates of a chunk.
         *
         * @param location The location of the chunk.
         * @return An array of doubles containing the x and z coordinates.
         */
        private double[] getChunkCoords(Location location) {
            return getChunkCoords(location.getChunk());
        }

        /**
         * This can be used to retrieve the x and z coordinates of a chunk.
         *
         * @param chunk The chunk to retrieve the coordinates of.
         * @return An array of doubles containing the x and z coordinates.
         */
        private double[] getChunkCoords(Chunk chunk) {
            double[] chunkCoords = new double[2];
            chunkCoords[0] = chunk.getX();
            chunkCoords[1] = chunk.getZ();
            return chunkCoords;
        }

        /**
         * Checks if a chunk is claimed.
         *
         * @param x     The x coordinate of the chunk.
         * @param y     The y coordinate of the chunk.
         * @param world The world that the chunk is in.
         * @return The claimed chunk if the chunk is claimed, and null if it is not.
         */
        private ClaimedChunk isChunkClaimed(double x, double y, String world) {
            for (ClaimedChunk chunk : claimedChunkRepository.all()) {
                if (x == chunk.getCoordinates()[0] && y == chunk.getCoordinates()[1] && world.equalsIgnoreCase(chunk.getWorldName())) {
                    return chunk;
                }
            }

            return null;
        }

        /**
         * This can be utilized to remove a claimed chunk from persistent data.
         *
         * @param chunkToRemove    The chunk to remove.
         * @param unclaimingPlayer The player removing the chunk.
         * @param holdingFaction   The faction that the chunk is owned by.
         */
        private void removeChunk(ClaimedChunk chunkToRemove, Player unclaimingPlayer, Faction holdingFaction) {
            // String identifier = (int)chunk.getChunk().getX() + "_" + (int)chunk.getChunk().getZ();

            // handle faction unclaim event calling and cancellation
            FactionUnclaimEvent unclaimEvent = new FactionUnclaimEvent(holdingFaction, unclaimingPlayer, chunkToRemove.getChunk());
            Bukkit.getPluginManager().callEvent(unclaimEvent);
            if (unclaimEvent.isCancelled()) {
                logger.debug("Unclaim event was cancelled.");
                return;
            }

            // get player's faction
            Faction playersFaction = getPlayersFaction(unclaimingPlayer.getUniqueId());

            // ensure that the claimed chunk is owned by the player's faction
            if (!chunkToRemove.getHolder().equals(playersFaction.getName())) {
                // TODO: add locale message
                return;
            }

            // if faction home is located on this chunk
            Location factionHome = holdingFaction.getFactionHome();
            if (factionHome != null) {
                if (factionHome.getChunk().getX() == chunkToRemove.getChunk().getX() && factionHome.getChunk().getZ() == chunkToRemove.getChunk().getZ()
                        && chunkToRemove.getWorldName().equalsIgnoreCase(Objects.requireNonNull(unclaimingPlayer.getLocation().getWorld()).getName())) {
                    // remove faction home
                    holdingFaction.setFactionHome(null);
                    messenger.sendAllPlayersInFactionMessage(holdingFaction, playerService.decideWhichMessageToUse(ChatColor.RED + localeService.get("AlertFactionHomeRemoved"), messageService.getLanguage().getString("AlertFactionHomeRemoved")));

                }
            }

            // remove locks on this chunk
            lockedBlockRepository.all().removeIf(block -> chunkToRemove.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunkToRemove.getChunk().getX() &&
                    chunkToRemove.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunkToRemove.getChunk().getZ() &&
                    block.getWorld().equalsIgnoreCase(chunkToRemove.getWorldName()));

            // remove any gates in this chunk
            Iterator<Gate> gtr = holdingFaction.getGates().iterator();
            while (gtr.hasNext()) {
                Gate gate = gtr.next();
                if (isGateInChunk(gate, chunkToRemove)) {
                    holdingFaction.removeGate(gate);
                    gtr.remove();
                }
            }

            claimedChunkRepository.delete(chunkToRemove);
        }

        /**
         * This can be utilized to get a chunk locationally relative to another chunk.
         *
         * @param origin    The chunk we are checking.
         * @param direction The direction the chunk we want to grab is.
         */
        private Chunk getChunkByDirection(Chunk origin, String direction) {

            int x = -1;
            int z = -1;

            if (direction.equalsIgnoreCase("north")) {
                x = origin.getX();
                z = origin.getZ() + 1;
            }
            if (direction.equalsIgnoreCase("east")) {
                x = origin.getX() + 1;
                z = origin.getZ();
            }
            if (direction.equalsIgnoreCase("south")) {
                x = origin.getX();
                z = origin.getZ() - 1;
            }
            if (direction.equalsIgnoreCase("west")) {
                x = origin.getX() - 1;
                z = origin.getZ();
            }

            return origin.getWorld().getChunkAt(x, z);
        }

        /**
         * Checks if the chunks to the North, East, South and West of the target are claimed by the same faction
         *
         * @param target The claimed chunk to check the neighbors of.
         * @return Boolean indicating whether or not the claimed chunk is surrounded.
         */
        private boolean isClaimedChunkSurroundedByChunksClaimedBySameFaction(ClaimedChunk target) {
            ClaimedChunk northernClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "north"));
            ClaimedChunk easternClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "east"));
            ClaimedChunk southernClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "south"));
            ClaimedChunk westernClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "west"));

            if (northernClaimedChunk == null ||
                    easternClaimedChunk == null ||
                    southernClaimedChunk == null ||
                    westernClaimedChunk == null) {

                return false;

            }

            boolean northernChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(northernClaimedChunk.getHolder());
            boolean easternChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(easternClaimedChunk.getHolder());
            boolean southernChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(southernClaimedChunk.getHolder());
            boolean westernChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(westernClaimedChunk.getHolder());

            return (northernChunkClaimedBySameFaction &&
                    easternChunkClaimedBySameFaction &&
                    southernChunkClaimedBySameFaction &&
                    westernChunkClaimedBySameFaction);
        }

        /**
         * Checks whether a block is able to be interacted with when taking into account the claiming system.
         *
         * @param event The PlayerInteractEvent event.
         * @return A boolean signifying whether the block is able to be interacted with.
         */
        private boolean canBlockBeInteractedWith(PlayerInteractEvent event) {
            if (event.getClickedBlock() != null) {
                // CHEST
                if (blockChecker.isChest(event.getClickedBlock())) {
                    return false;
                }
                switch (event.getClickedBlock().getType()) {
                    case ACACIA_DOOR:
                    case BIRCH_DOOR:
                    case DARK_OAK_DOOR:
                    case IRON_DOOR:
                    case JUNGLE_DOOR:
                    case OAK_DOOR:
                    case SPRUCE_DOOR:
                    case ACACIA_TRAPDOOR:
                    case BIRCH_TRAPDOOR:
                    case DARK_OAK_TRAPDOOR:
                    case IRON_TRAPDOOR:
                    case JUNGLE_TRAPDOOR:
                    case OAK_TRAPDOOR:
                    case SPRUCE_TRAPDOOR:
                    case ACACIA_FENCE_GATE:
                    case BIRCH_FENCE_GATE:
                    case DARK_OAK_FENCE_GATE:
                    case JUNGLE_FENCE_GATE:
                    case OAK_FENCE_GATE:
                    case SPRUCE_FENCE_GATE:
                    case BARREL:
                    case LEVER:
                    case ACACIA_BUTTON:
                    case BIRCH_BUTTON:
                    case DARK_OAK_BUTTON:
                    case JUNGLE_BUTTON:
                    case OAK_BUTTON:
                    case SPRUCE_BUTTON:
                    case STONE_BUTTON:
                    case LECTERN:
                        return false;
                    default:
                        break;
                }
            }
            return true;
        }

        /**
         * This can be utilized to find out what materials are allowed to be used in a faction's territory regardless of member status.
         *
         * @param material The material to check.
         * @return Whether the material can be used.
         */
        private boolean materialAllowed(Material material) {
            switch (material) {
                case BREAD:
                case POTATO:
                case CARROT:
                case BEETROOT:
                case BEEF:
                case PORKCHOP:
                case CHICKEN:
                case COD:
                case SALMON:
                case MUTTON:
                case RABBIT:
                case TROPICAL_FISH:
                case PUFFERFISH:
                case MUSHROOM_STEW:
                case RABBIT_STEW:
                case BEETROOT_SOUP:
                case COOKED_BEEF:
                case COOKED_PORKCHOP:
                case COOKED_CHICKEN:
                case COOKED_SALMON:
                case COOKED_MUTTON:
                case COOKED_COD:
                case MELON:
                case PUMPKIN:
                case MELON_SLICE:
                case CAKE:
                case PUMPKIN_PIE:
                case APPLE:
                case COOKIE:
                case POISONOUS_POTATO:
                case CHORUS_FRUIT:
                case DRIED_KELP:
                case BAKED_POTATO:
                default:
                    break;
            }
            return true;
        }
    }

    /**
     * @author Daniel McCoy Stephenson
     * @author Pasarus
     */
    public class LocalStorageService {
        private final static String FILE_PATH = "./plugins/MedievalFactions/";
        private final static String FACTIONS_FILE_NAME = "factions.json";
        private final static String CHUNKS_FILE_NAME = "claimedchunks.json";
        private final static String PLAYERPOWER_FILE_NAME = "playerpowerrecords.json";
        private final static String PLAYERACTIVITY_FILE_NAME = "playeractivityrecords.json";
        private final static String LOCKED_BLOCKS_FILE_NAME = "lockedblocks.json";
        private final PersistentData persistentData;
        //        private final static String WARS_FILE_NAME = "wars.json";
        private final Type LIST_MAP_TYPE = new TypeToken<ArrayList<HashMap<String, String>>>() {
        }.getType();

        private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        public LocalStorageService(PersistentData persistentData) {
            this.persistentData = persistentData;
        }

        public void save() {
            saveFactions();
            saveClaimedChunks();
            savePlayerRecords();
            saveLockedBlocks();
            saveWars();
            if (configService.hasBeenAltered()) {
                medievalFactions.saveConfig();
            }
        }

        public void load() {
            loadFactions();
            loadClaimedChunks();
            loadPlayerRecords();
            loadLockedBlocks();
            loadWars();
        }

        private void saveFactions() {
            factionRepository.persist();
        }

        private void saveClaimedChunks() {
            claimedChunkRepository.persist();
        }

        private void savePlayerRecords() {
            playerRecordRepository.persist();
        }

        private void saveLockedBlocks() {
            lockedBlockRepository.persist();
        }

        private void saveWars() {
//            List<Map<String, String>> warsToSave = new ArrayList<>();
//            for (War war : wars) {
//                warsToSave.add(war.save());
//            }
//
//            File file = new File(FILE_PATH + WARS_FILE_NAME);
//            writeOutFiles(file, warsToSave);
        }

        private void writeOutFiles(File file, List saveData) {
            this.writeOutFiles(file, gson.toJson(saveData));
        }

        private void writeOutFiles(File file, String saveData) {
            try {
                file.createNewFile();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
                outputStreamWriter.write(saveData);
                outputStreamWriter.close();
            } catch (IOException e) {
                System.out.println("ERROR: " + e);
            }
        }

        private void loadFactions() {
            factionRepository.load();
        }

        private void loadClaimedChunks() {
            claimedChunkRepository.load();
        }

        private void loadPlayerRecords() {
            playerRecordRepository.load();
        }

        private void loadLockedBlocks() {
            lockedBlockRepository.load();
        }

        private void loadWars() {
//            wars.clear();
//
//            ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + WARS_FILE_NAME);
//
//            for (Map<String, String> warData : data) {
//                War war = new War(warData);
//                addWar(war);
//            }
        }

        private ArrayList<HashMap<String, String>> loadDataFromFilename(String filename) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8));
                return gson.fromJson(reader, LIST_MAP_TYPE);
            } catch (FileNotFoundException ignored) {

            }
            return new ArrayList<>();
        }

    }
}