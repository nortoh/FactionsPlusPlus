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

import factionsplusplus.events.internal.FactionClaimEvent;
import factionsplusplus.events.internal.FactionUnclaimEvent;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionBase;
import factionsplusplus.models.Gate;
import factionsplusplus.models.FPPPlayer;
import factionsplusplus.utils.BlockUtils;
import factionsplusplus.utils.ChunkUtils;
import factionsplusplus.utils.InteractionAccessChecker;
import factionsplusplus.utils.Logger;

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
    private final DataService dataService;
    private final InteractionAccessChecker interactionAccessChecker;
    private final Logger logger;

    @Inject
    public ClaimService(
        ConfigService configService,
        DataService dataService,
        InteractionAccessChecker interactionAccessChecker,
        Logger logger
    ) {
        this.configService = configService;
        this.dataService = dataService;
        this.interactionAccessChecker = interactionAccessChecker;
        this.logger = logger;
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
        int maxClaimRadius = this.configService.getInt("faction.limits.claim.radius");

        FPPPlayer member = this.dataService.getPlayer(claimant.getUniqueId());

        // check if depth is valid
        if (depth < 0 || depth > maxClaimRadius) {
            member.error("Error.Claim.Radius", maxClaimRadius);
            return;
        }

        if (! this.dataService.getWorld(location.getWorld().getUID()).getFlag("allowClaims").toBoolean()) {
            member.error("Error.Claim.DisabledForWorld");
            return;
        }

        // if depth is 0, we just need to claim the chunk the player is on
        if (depth == 0) {
            this.claimChunkAtLocation(claimant, location, claimantsFaction);
            return;
        }

        // claim chunks
        final Chunk initial = location.getChunk();
        final Set<Chunk> chunkSet = ChunkUtils.obtainChunksForRadius(initial, depth);
        chunkSet.forEach(chunk -> this.claimChunkAtLocation(
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
            this.dataService.getPlayer(player.getUniqueId()).error("RadiusRequirement", maxChunksUnclaimable);
            return;
        }

        // unclaim chunks
        final Set<Chunk> chunkSet = ChunkUtils.obtainChunksForRadius(player.getLocation().getChunk(), radius);
        chunkSet.stream()
                .map(c -> this.dataService.getClaimedChunk(c.getX(), c.getZ(), c.getWorld().getUID()))
                .filter(Objects::nonNull)
                .forEach(chunk -> this.removeChunk(chunk, player, faction));
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
        this.claimChunkAtLocation(claimant, chunkCoords, location.getWorld(), claimantsFaction);
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

        FPPPlayer member = this.dataService.getPlayer(player.getUniqueId());

        // handle admin bypass
        if (this.dataService.getPlayer(player.getUniqueId()).isAdminBypassing()) {
            ClaimedChunk chunk = this.dataService.getClaimedChunk(playerCoords[0], playerCoords[1], Objects.requireNonNull(player.getLocation().getWorld()).getUID());
            if (chunk != null) {
                this.removeChunk(chunk, player, this.dataService.getFaction(chunk.getHolder()));
                member.success("CommandResponse.LandUnclaimedForcefully");
                return;
            }
            member.error("Error.Claim.NotClaimed");
            return;
        }

        ClaimedChunk chunk = this.dataService.getClaimedChunk(playerCoords[0], playerCoords[1], Objects.requireNonNull(player.getLocation().getWorld()).getUID());

        // ensure that chunk is claimed
        if (chunk == null) {
            return;
        }

        // ensure that the chunk is claimed by the player's faction.
        if (! chunk.getHolder().equals(playersFaction.getUUID())) {
            Faction chunkOwner = this.dataService.getFaction(chunk.getHolder());
            member.error("CommandResponse.LandClaimedBy", chunkOwner.getName());
            return;
        }

        // initiate removal
        this.removeChunk(chunk, player, playersFaction);
        member.success("CommandResponse.LandUnclaimed");
    }

    /**
     * This can be used to check which faction has laid claim to a chunk.
     *
     * @param player The player whose location we will be checking.
     * @return The name of the faction that has claimed the chunk. A value of "unclaimed" will be returned if the chunk is unclaimed.
     */
    public Faction checkOwnershipAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        ClaimedChunk chunk = this.dataService.getClaimedChunk(playerCoords[0], playerCoords[1], Objects.requireNonNull(player.getLocation().getWorld()).getUID());
        if (chunk != null) {
            return this.dataService.getFactionRepository().get(chunk.getHolder());
        }
        return null;
    }

    /**
     * This can be used to check if a faction has more claimed land than power.
     *
     * @param faction The faction we are checking.
     * @return Whether the faction's claimed land exceeds their power.
     */
    public boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
        return (this.dataService.getClaimedChunksForFaction(faction).size() > faction.getCumulativePowerLevel());
    }

    /**
     * If a player is exceeding their demesne limit, this method will inform them.
     *
     * @param player The player to inform.
     */
    public void informPlayerIfTheirLandIsInDanger(Player player) {
        Faction faction = this.dataService.getPlayersFaction(player.getUniqueId());
        if (faction != null) {
            if (this.isFactionExceedingTheirDemesneLimit(faction)) {
                this.dataService.getPlayer(player.getUniqueId()).alert("FactionNotice.ExcessClaims");
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
        if (! this.dataService.isPlayerInFaction(event.getPlayer()) && ! this.dataService.getPlayer(event.getPlayer().getUniqueId()).isAdminBypassing()) {

            Block block = event.getClickedBlock();
            if (this.configService.getBoolean("faction.protections.nonMembersCanInteractWithDoors") && block != null && BlockUtils.isDoor(block)) {
                // allow non-faction members to interact with doors
                return;
            }

            event.setCancelled(true);
        }

        // TODO: simplify this code with a call to the shouldEventBeCancelled() method in InteractionAccessChecker.java

        final Faction playersFaction = this.dataService.getPlayersFaction(event.getPlayer().getUniqueId());
        if (playersFaction == null) {
            return;
        }

        // if player's faction is not the same as the holder of the chunk and player isn't overriding
        if (! (playersFaction.getUUID().equals(claimedChunk.getHolder())) && ! this.dataService.getPlayer(event.getPlayer().getUniqueId()).isAdminBypassing()) {

            Block block = event.getClickedBlock();
            if (this.configService.getBoolean("faction.protections.nonMembersCanInteractWithDoors") && block != null && BlockUtils.isDoor(block)) {
                // allow non-faction members to interact with doors
                return;
            }

            // if enemy territory
            if (playersFaction.isEnemy(claimedChunk.getHolder())) {
                // if not interacting with chest
                if (this.canBlockBeInteractedWith(event)) {
                    // allow placing ladders
                    if (this.configService.getBoolean("faction.protections.laddersPlaceableByEnemies")) {
                        if (event.getMaterial() == LADDER) {
                            return;
                        }
                    }
                    // allow eating
                    if (this.materialAllowed(event.getMaterial())) {
                        return;
                    }
                    // allow blocking
                    if (event.getPlayer().getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                        return;
                    }
                }
            }

            if (! this.interactionAccessChecker.isOutsiderInteractionAllowed(event.getPlayer(), claimedChunk, playersFaction)) {
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

        ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(location.getChunk());

        if (claimedChunk != null) {
            this.removeChunk(claimedChunk, player, faction);
        }

        this.addClaimedChunk(location.getChunk(), faction, Objects.requireNonNull(location.getWorld()));
    }

    private void claimChunkAtLocation(Player claimant, double[] chunkCoords, World world, Faction claimantsFaction) {

        FPPPlayer member = this.dataService.getPlayer(claimant.getUniqueId());

        // if demesne limit enabled
        if (this.configService.getBoolean("faction.limitLandByPower")) {
            // if at demesne limit
            
            if (! (this.dataService.getClaimedChunksForFaction(claimantsFaction).size() < claimantsFaction.getCumulativePowerLevel())) {
                member.error("FactionNotice.ReachedDemesne");
                return;
            }
        }

        // check if land is already claimed
        ClaimedChunk chunk = this.dataService.getClaimedChunk(chunkCoords[0], chunkCoords[1], world.getUID());
        
        if (chunk != null) {
            // chunk already claimed
            Faction targetFaction = this.dataService.getFaction(chunk.getHolder());

            // if holder is player's faction
            if (targetFaction.getName().equalsIgnoreCase(claimantsFaction.getName()) && ! claimantsFaction.shouldAutoClaim()) {
                member.error("Error.Claim.FactionClaimedAlready");
                return;
            }

            // if not at war with target faction
            if (! claimantsFaction.isEnemy(targetFaction.getUUID())) {
                member.error("Error.Conquer.NotEnemies", targetFaction.getName());
                return;
            }

            // surrounded chunk protection check
            if (this.configService.getBoolean("faction.protections.claims.protectSurroundedChunks")) {
                if (this.isClaimedChunkSurroundedByChunksClaimedBySameFaction(chunk)) {
                    member.error("Error.Conquer.Surrounded", targetFaction.getName());
                    return;
                }
            }

            double targetFactionsCumulativePowerLevel = targetFaction.getCumulativePowerLevel();
            int chunksClaimedByTargetFaction = this.dataService.getClaimedChunksForFaction(targetFaction).size();

            // if target faction does not have more land than their demesne limit
            if (! (targetFactionsCumulativePowerLevel < chunksClaimedByTargetFaction)) {
                member.error("Error.Conquer.NotOverLimit", targetFaction.getName());
                return;
            }

            // CONQUERABLE

            // remove locks on this chunk
            this.dataService.getLockedBlockRepository().all().removeIf(block -> chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                    chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ());

            FactionClaimEvent claimEvent = new FactionClaimEvent(claimantsFaction, claimant, chunk.getChunk());
            Bukkit.getPluginManager().callEvent(claimEvent);
            if (! claimEvent.isCancelled()) {
                this.dataService.getClaimedChunkRepository().delete(chunk);

                Chunk toClaim = world.getChunkAt((int) chunkCoords[0], (int) chunkCoords[1]);
                this.addClaimedChunk(toClaim, claimantsFaction, claimant.getWorld());
                member.success("CommandResponse.LandConquered", targetFaction.getName(), this.dataService.getClaimedChunksForFaction(claimantsFaction).size(), claimantsFaction.getCumulativePowerLevel());
                targetFaction.alert("FactionNotice.LandConquered", claimantsFaction.getName());
            }
        } else {
            Chunk toClaim = world.getChunkAt((int) chunkCoords[0], (int) chunkCoords[1]);
            FactionClaimEvent claimEvent = new FactionClaimEvent(claimantsFaction, claimant, toClaim);
            Bukkit.getPluginManager().callEvent(claimEvent);
            if (! claimEvent.isCancelled()) {
                // chunk not already claimed
                this.addClaimedChunk(toClaim, claimantsFaction, claimant.getWorld());
                member.success("CommandResponse.LandClaimed", this.dataService.getClaimedChunksForFaction(claimantsFaction).size(), claimantsFaction.getCumulativePowerLevel());
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
        ClaimedChunk newChunk = new ClaimedChunk(chunk, faction.getUUID());
        this.dataService.addClaimedChunk(newChunk);       
    }

    /**
     * This can be utilized to remove a claimed chunk from persistent data.
     *
     * @param chunkToRemove    The chunk to remove.
     * @param unclaimingPlayer The player removing the chunk.
     * @param holdingFaction   The faction that the chunk is owned by.
     */
    private void removeChunk(ClaimedChunk chunkToRemove, Player unclaimingPlayer, Faction holdingFaction) {
        // handle faction unclaim event calling and cancellation
        FactionUnclaimEvent unclaimEvent = new FactionUnclaimEvent(holdingFaction, unclaimingPlayer, chunkToRemove.getChunk());
        Bukkit.getPluginManager().callEvent(unclaimEvent);
        if (unclaimEvent.isCancelled()) {
            logger.debug("Unclaim event was cancelled.");
            return;
        }

        // get player's faction
        Faction playersFaction = this.dataService.getPlayersFaction(unclaimingPlayer.getUniqueId());

        // ensure that the claimed chunk is owned by the player's faction
        if (! chunkToRemove.getHolder().equals(playersFaction.getUUID())) {
            this.dataService.getPlayer(unclaimingPlayer.getUniqueId()).alert("Error.Claim.NotOwnLand");
            return;
        }

        // if a faction base is located on this chunk
        for (FactionBase base : holdingFaction.getBases().values()) {
            Chunk baseChunk = base.getBukkitLocation().getChunk();
            if (
                baseChunk.getX() == chunkToRemove.getChunk().getX() && 
                baseChunk.getZ() == chunkToRemove.getChunk().getZ() &&
                chunkToRemove.getWorldUUID().equals(baseChunk.getWorld().getUID())
            ) {
                holdingFaction.removeBase(base.getName());
                holdingFaction.alert("FactionNotice.Base.Removed", base.getName());
            }
        }

        // remove locks on this chunk
        this.dataService.getLockedBlockRepository().all().removeIf(block -> chunkToRemove.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunkToRemove.getChunk().getX() &&
                chunkToRemove.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunkToRemove.getChunk().getZ() &&
                block.getWorld().equals(chunkToRemove.getWorldUUID()));

        // remove any gates in this chunk
        Iterator<Gate> gtr = this.dataService.getFactionsGates(holdingFaction).iterator();
        while (gtr.hasNext()) {
            Gate gate = gtr.next();
            if (ChunkUtils.isGateInChunk(gate, chunkToRemove)) {
                this.dataService.removeGate(gate);
                gtr.remove();
            }
        }

        this.dataService.deleteClaimedChunk(chunkToRemove);
    }

    /**
     * Checks if the chunks to the North, East, South and West of the target are claimed by the same faction
     *
     * @param target The claimed chunk to check the neighbors of.
     * @return Boolean indicating whether or not the claimed chunk is surrounded.
     */
    private boolean isClaimedChunkSurroundedByChunksClaimedBySameFaction(ClaimedChunk target) {
        ClaimedChunk northernClaimedChunk = this.dataService.getClaimedChunk(ChunkUtils.getChunkByDirection(target.getChunk(), "north"));
        ClaimedChunk easternClaimedChunk = this.dataService.getClaimedChunk(ChunkUtils.getChunkByDirection(target.getChunk(), "east"));
        ClaimedChunk southernClaimedChunk = this.dataService.getClaimedChunk(ChunkUtils.getChunkByDirection(target.getChunk(), "south"));
        ClaimedChunk westernClaimedChunk = this.dataService.getClaimedChunk(ChunkUtils.getChunkByDirection(target.getChunk(), "west"));

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
            if (BlockUtils.isChest(event.getClickedBlock())) {
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