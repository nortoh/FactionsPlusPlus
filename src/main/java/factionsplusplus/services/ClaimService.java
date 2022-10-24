package factionsplusplus.services;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.events.FactionClaimEvent;
import factionsplusplus.events.FactionUnclaimEvent;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.models.Gate;
import factionsplusplus.utils.ChunkUtils;
import factionsplusplus.utils.InteractionAccessChecker;
import factionsplusplus.utils.Logger;
import factionsplusplus.utils.extended.BlockChecker;

import java.util.Iterator;
import java.util.Set;
import java.util.Objects;

import static org.bukkit.Material.LADDER;

/**
 * This class assists in the management of claimed chunks.
 *
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class ClaimService {

    private final ConfigService configService;
    private final MessageService messageService;
    private final DataService dataService;
    private final EphemeralData ephemeralData;
    private final FactionService factionService;
    private final InteractionAccessChecker interactionAccessChecker;
    private final Logger logger;
    private final BlockChecker blockChecker;

    @Inject
    public ClaimService(
        ConfigService configService,
        MessageService messageService,
        DataService dataService,
        EphemeralData ephemeralData,
        FactionService factionService,
        InteractionAccessChecker interactionAccessChecker,
        Logger logger,
        BlockChecker blockChecker
    ) {
        this.configService = configService;
        this.messageService = messageService;
        this.dataService = dataService;
        this.ephemeralData = ephemeralData;
        this.factionService = factionService;
        this.interactionAccessChecker = interactionAccessChecker;
        this.logger = logger;
        this.blockChecker = blockChecker;
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
            messageService.sendLocalizedMessage(
                claimant,
                new MessageBuilder("RadiusRequirement")
                    .with("number", String.valueOf(maxClaimRadius))
            );
            return;
        }

        if (! this.dataService.getWorld(location.getWorld().getUID()).getFlag("allowClaims").toBoolean()) {
            this.messageService.sendLocalizedMessage(claimant, "ClaimsDisabled");
            return;
        }

        // if depth is 0, we just need to claim the chunk the player is on
        if (depth == 0) {
            claimChunkAtLocation(claimant, location, claimantsFaction);
            return;
        }

        // claim chunks
        final Chunk initial = location.getChunk();
        final Set<Chunk> chunkSet = ChunkUtils.obtainChunksForRadius(initial, depth);
        chunkSet.forEach(chunk -> claimChunkAtLocation(
                claimant, ChunkUtils.getChunkCoords(chunk), chunk.getWorld(), claimantsFaction
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
            messageService.sendLocalizedMessage(
                player,
                new MessageBuilder("RadiusRequirement")
                    .with("number", String.valueOf(maxChunksUnclaimable))
            );
            return;
        }

        // unclaim chunks
        final Set<Chunk> chunkSet = ChunkUtils.obtainChunksForRadius(player.getLocation().getChunk(), radius);
        chunkSet.stream()
                .map(c -> dataService.getClaimedChunk(c.getX(), c.getZ(), c.getWorld().getName()))
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
        double[] chunkCoords = ChunkUtils.getChunkCoords(location);
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
            ClaimedChunk chunk = dataService.getClaimedChunk(playerCoords[0], playerCoords[1], Objects.requireNonNull(player.getLocation().getWorld()).getName());
            if (chunk != null) {
                removeChunk(chunk, player, dataService.getFaction(chunk.getHolder()));
                messageService.sendLocalizedMessage(player, "LandClaimedUsingAdminBypass");
                return;
            }
            messageService.sendLocalizedMessage(player, "LandNotCurrentlyClaimed");
            return;
        }

        ClaimedChunk chunk = dataService.getClaimedChunk(playerCoords[0], playerCoords[1], Objects.requireNonNull(player.getLocation().getWorld()).getName());

        // ensure that chunk is claimed
        if (chunk == null) {
            return;
        }

        // ensure that the chunk is claimed by the player's faction.
        if (!chunk.getHolder().equals(playersFaction.getName())) {
            Faction chunkOwner = dataService.getFaction(chunk.getHolder());
            messageService.sendLocalizedMessage(
                player,
                new MessageBuilder("LandClaimedBy")
                    .with("player", chunkOwner.getName())
            );
            return;
        }

        // initiate removal
        removeChunk(chunk, player, playersFaction);
        messageService.sendLocalizedMessage(player, "LandUnclaimed");
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
        ClaimedChunk chunk = dataService.getClaimedChunk(playerCoords[0], playerCoords[1], Objects.requireNonNull(player.getLocation().getWorld()).getName());
        if (chunk != null) {
            return dataService.getFactionRepository().get(chunk.getHolder()).getName();
        }
        return "unclaimed";
    }

    /**
     * This can be used to check if a faction has more claimed land than power.
     *
     * @param faction The faction we are checking.
     * @return Whether the faction's claimed land exceeds their power.
     */
    public boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
        return (dataService.getClaimedChunksForFaction(faction).size() > factionService.getCumulativePowerLevel(faction));
    }

    /**
     * If a player is exceeding their demesne limit, this method will inform them.
     *
     * @param player The player to inform.
     */
    public void informPlayerIfTheirLandIsInDanger(Player player) {
        Faction faction = dataService.getPlayersFaction(player.getUniqueId());
        if (faction != null) {
            if (isFactionExceedingTheirDemesneLimit(faction)) {
                messageService.sendLocalizedMessage(player, "AlertMoreClaimedChunksThanPower");
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
        if (!dataService.isPlayerInFaction(event.getPlayer()) && !ephemeralData.getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

            Block block = event.getClickedBlock();
            if (configService.getBoolean("nonMembersCanInteractWithDoors") && block != null && blockChecker.isDoor(block)) {
                // allow non-faction members to interact with doors
                return;
            }

            event.setCancelled(true);
        }

        // TODO: simplify this code with a call to the shouldEventBeCancelled() method in InteractionAccessChecker.java

        final Faction playersFaction = dataService.getPlayersFaction(event.getPlayer().getUniqueId());
        if (playersFaction == null) {
            return;
        }

        // if player's faction is not the same as the holder of the chunk and player isn't overriding
        if (!(playersFaction.getID().equals(claimedChunk.getHolder())) && !ephemeralData.getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

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

        ClaimedChunk claimedChunk = dataService.getClaimedChunk(location.getChunk());

        if (claimedChunk != null) {
            removeChunk(claimedChunk, player, faction);
        }

        addClaimedChunk(location.getChunk(), faction, Objects.requireNonNull(location.getWorld()));
    }

    private void claimChunkAtLocation(Player claimant, double[] chunkCoords, World world, Faction claimantsFaction) {

        // if demesne limit enabled
        if (configService.getBoolean("limitLand")) {
            // if at demesne limit
            
            if (!(dataService.getClaimedChunksForFaction(claimantsFaction).size() < factionService.getCumulativePowerLevel(claimantsFaction))) {
                System.out.println("Limit reached");
                messageService.sendLocalizedMessage(claimant, "AlertReachedDemesne");
                return;
            }
        }

        // check if land is already claimed
        ClaimedChunk chunk = dataService.getClaimedChunk(chunkCoords[0], chunkCoords[1], world.getName());
        if (chunk != null) {
            // chunk already claimed
            Faction targetFaction = dataService.getFaction(chunk.getHolder());

            // if holder is player's faction
            if (targetFaction.getName().equalsIgnoreCase(claimantsFaction.getName()) && !claimantsFaction.getAutoClaimStatus()) {
                messageService.sendLocalizedMessage(claimant, "LandAlreadyClaimedByYourFaction");
                return;
            }

            // if not at war with target faction
            if (!claimantsFaction.isEnemy(targetFaction.getID())) {
                messageService.sendLocalizedMessage(claimant, "IsNotEnemy");
                return;
            }

            // surrounded chunk protection check
            if (configService.getBoolean("surroundedChunksProtected")) {
                if (isClaimedChunkSurroundedByChunksClaimedBySameFaction(chunk)) {
                    messageService.sendLocalizedMessage(claimant, "SurroundedChunkProtected");
                    return;
                }
            }

            int targetFactionsCumulativePowerLevel = factionService.getCumulativePowerLevel(targetFaction);
            int chunksClaimedByTargetFaction = dataService.getClaimedChunksForFaction(targetFaction).size();

            // if target faction does not have more land than their demesne limit
            if (!(targetFactionsCumulativePowerLevel < chunksClaimedByTargetFaction)) {
                messageService.sendLocalizedMessage(claimant, "TargetFactionNotOverClaiming");
                return;
            }

            // CONQUERABLE

            // remove locks on this chunk
            dataService.getLockedBlockRepository().all().removeIf(block -> chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                    chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ());

            FactionClaimEvent claimEvent = new FactionClaimEvent(claimantsFaction, claimant, chunk.getChunk());
            Bukkit.getPluginManager().callEvent(claimEvent);
            if (!claimEvent.isCancelled()) {
                dataService.getClaimedChunkRepository().delete(chunk);

                Chunk toClaim = world.getChunkAt((int) chunkCoords[0], (int) chunkCoords[1]);
                addClaimedChunk(toClaim, claimantsFaction, claimant.getWorld());
                messageService.sendLocalizedMessage(
                    claimant,
                    new MessageBuilder("AlertLandConqueredFromAnotherFaction")
                        .with("name", targetFaction.getName())
                        .with("number", String.valueOf(dataService.getClaimedChunksForFaction(claimantsFaction).size()))
                        .with("max", String.valueOf(factionService.getCumulativePowerLevel(claimantsFaction)))
                );
                messageService.sendFactionLocalizedMessage(
                    targetFaction,
                    new MessageBuilder("AlertLandConqueredFromYourFaction")
                        .with("name", claimantsFaction.getName())
                );
            }
        } else {
            Chunk toClaim = world.getChunkAt((int) chunkCoords[0], (int) chunkCoords[1]);
            FactionClaimEvent claimEvent = new FactionClaimEvent(claimantsFaction, claimant, toClaim);
            Bukkit.getPluginManager().callEvent(claimEvent);
            if (!claimEvent.isCancelled()) {
                // chunk not already claimed
                addClaimedChunk(toClaim, claimantsFaction, claimant.getWorld());
                messageService.sendLocalizedMessage(
                    claimant,
                    new MessageBuilder("AlertLandClaimed")
                        .with("number", String.valueOf(dataService.getClaimedChunksForFaction(claimantsFaction).size()))
                        .with("max", String.valueOf(factionService.getCumulativePowerLevel(claimantsFaction)))
                );
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
        newChunk.setHolder(faction.getID());
        dataService.getClaimedChunkRepository().create(newChunk);            
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
        Faction playersFaction = dataService.getPlayersFaction(unclaimingPlayer.getUniqueId());

        // ensure that the claimed chunk is owned by the player's faction
        if (!chunkToRemove.getHolder().equals(playersFaction.getID())) {
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
                messageService.sendFactionLocalizedMessage(holdingFaction, "AlertFactionHomeRemoved");
            }
        }

        // remove locks on this chunk
        dataService.getLockedBlockRepository().all().removeIf(block -> chunkToRemove.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunkToRemove.getChunk().getX() &&
                chunkToRemove.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunkToRemove.getChunk().getZ() &&
                block.getWorld().equalsIgnoreCase(chunkToRemove.getWorldName()));

        // remove any gates in this chunk
        Iterator<Gate> gtr = holdingFaction.getGates().iterator();
        while (gtr.hasNext()) {
            Gate gate = gtr.next();
            if (ChunkUtils.isGateInChunk(gate, chunkToRemove)) {
                holdingFaction.removeGate(gate);
                gtr.remove();
            }
        }

        dataService.getClaimedChunkRepository().delete(chunkToRemove);
    }

    /**
     * Checks if the chunks to the North, East, South and West of the target are claimed by the same faction
     *
     * @param target The claimed chunk to check the neighbors of.
     * @return Boolean indicating whether or not the claimed chunk is surrounded.
     */
    private boolean isClaimedChunkSurroundedByChunksClaimedBySameFaction(ClaimedChunk target) {
        ClaimedChunk northernClaimedChunk = dataService.getClaimedChunk(ChunkUtils.getChunkByDirection(target.getChunk(), "north"));
        ClaimedChunk easternClaimedChunk = dataService.getClaimedChunk(ChunkUtils.getChunkByDirection(target.getChunk(), "east"));
        ClaimedChunk southernClaimedChunk = dataService.getClaimedChunk(ChunkUtils.getChunkByDirection(target.getChunk(), "south"));
        ClaimedChunk westernClaimedChunk = dataService.getClaimedChunk(ChunkUtils.getChunkByDirection(target.getChunk(), "west"));

        if (northernClaimedChunk == null ||
                easternClaimedChunk == null ||
                southernClaimedChunk == null ||
                westernClaimedChunk == null) {

            return false;

        }

        boolean northernChunkClaimedBySameFaction = target.getHolder().equals(northernClaimedChunk.getHolder());
        boolean easternChunkClaimedBySameFaction = target.getHolder().equals(easternClaimedChunk.getHolder());
        boolean southernChunkClaimedBySameFaction = target.getHolder().equals(southernClaimedChunk.getHolder());
        boolean westernChunkClaimedBySameFaction = target.getHolder().equals(westernClaimedChunk.getHolder());

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