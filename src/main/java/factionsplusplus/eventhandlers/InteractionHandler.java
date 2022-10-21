/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.PersistentData;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.services.*;
import factionsplusplus.utils.InteractionAccessChecker;
import factionsplusplus.utils.extended.BlockChecker;
import factionsplusplus.builders.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.Objects;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class InteractionHandler implements Listener {
    private final PersistentData persistentData;
    private final InteractionAccessChecker interactionAccessChecker;
    private final MessageService messageService;
    private final LocaleService localeService;
    private final BlockChecker blockChecker;
    private final FactionsPlusPlus factionsPlusPlus;
    private final LockService lockService;
    private final EphemeralData ephemeralData;
    private final GateService gateService;
    private final DataService dataService;

    @Inject
    public InteractionHandler(DataService dataService, PersistentData persistentData, InteractionAccessChecker interactionAccessChecker, LocaleService localeService, BlockChecker blockChecker, FactionsPlusPlus factionsPlusPlus, LockService lockService, EphemeralData ephemeralData, GateService gateService, MessageService messageService) {
        this.dataService = dataService;
        this.persistentData = persistentData;
        this.interactionAccessChecker = interactionAccessChecker;
        this.localeService = localeService;
        this.blockChecker = blockChecker;
        this.factionsPlusPlus = factionsPlusPlus;
        this.lockService = lockService;
        this.ephemeralData = ephemeralData;
        this.gateService = gateService;
        this.messageService = messageService;
    }

    @EventHandler()
    public void handle(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(block.getLocation().getChunk());

        if (interactionAccessChecker.shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
            return;
        }

        if (persistentData.isBlockInGate(block, player)) {
            event.setCancelled(true);
            return;
        }

        if (this.dataService.isBlockLocked(block)) {
            boolean isOwner = this.dataService.getLockedBlock(block).getOwner().equals(player.getUniqueId());
            if (!isOwner) {
                event.setCancelled(true);
                messageService.sendLocalizedMessage(player, "AlertNonOwnership");
                return;
            }

            this.dataService.getLockedBlockRepository().delete(block);

            if (blockChecker.isDoor(block)) {
                removeLocksAboveAndBelowTheOriginalBlockAsWell(block);
            }
        }
    }

    private void removeLocksAboveAndBelowTheOriginalBlockAsWell(Block block) {

        Block relativeUp = block.getRelative(BlockFace.UP);
        Block relativeDown = block.getRelative(BlockFace.DOWN);
        if (blockChecker.isDoor(relativeUp)) {
            this.dataService.getLockedBlockRepository().delete(relativeUp);
        }
        if (blockChecker.isDoor(relativeDown)) {
            this.dataService.getLockedBlockRepository().delete(relativeDown);
        }
    }

    @EventHandler()
    public void handle(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(event.getBlock().getLocation().getChunk());

        if (interactionAccessChecker.isPlayerAttemptingToPlaceLadderInEnemyTerritoryAndIsThisAllowed(event.getBlockPlaced(), player, claimedChunk)) {
            return;
        }

        if (interactionAccessChecker.shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
            return;
        }

        if (blockChecker.isChest(event.getBlock())) {
            boolean isNextToNonOwnedLockedChest = blockChecker.isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock());
            if (isNextToNonOwnedLockedChest) {
                messageService.sendLocalizedMessage(player, "CannotPlaceChestsNextToUnownedLockedChests");
                event.setCancelled(true);
                return;
            }

            int seconds = 2;
            factionsPlusPlus.getServer().getScheduler().runTaskLater(factionsPlusPlus, () -> {
                Block block = player.getWorld().getBlockAt(event.getBlock().getLocation());

                if (!blockChecker.isChest(block)) {
                    // There has been 2 seconds since we last confirmed this was a chest, double-checking isn't ever bad :)
                    return;
                }

                InventoryHolder holder = ((Chest) block.getState()).getInventory().getHolder();
                if (holder instanceof DoubleChest) {
                    // make sure both sides are locked
                    DoubleChest doubleChest = (DoubleChest) holder;
                    Block leftChest = ((Chest) Objects.requireNonNull(doubleChest.getLeftSide())).getBlock();
                    Block rightChest = ((Chest) Objects.requireNonNull(doubleChest.getRightSide())).getBlock();

                    if (this.dataService.isBlockLocked(leftChest)) {
                        // lock right chest
                        LockedBlock right = new LockedBlock(player.getUniqueId(), this.dataService.getPlayersFaction(player.getUniqueId()).getID(), rightChest.getX(), rightChest.getY(), rightChest.getZ(), rightChest.getWorld().getName());
                        this.dataService.getLockedBlockRepository().create(right);
                    } else {
                        if (this.dataService.isBlockLocked(rightChest)) {
                            // lock left chest
                            LockedBlock left = new LockedBlock(player.getUniqueId(), this.dataService.getPlayersFaction(player.getUniqueId()).getID(), leftChest.getX(), leftChest.getY(), leftChest.getZ(), leftChest.getWorld().getName());
                            this.dataService.getLockedBlockRepository().create(left);
                        }
                    }

                }
            }, seconds * 20);
        }

        // if hopper
        if (event.getBlock().getType() == Material.HOPPER) {
            boolean isNextToNonOwnedLockedChest = blockChecker.isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock());
            boolean isUnderOrAboveNonOwnedLockedChest = blockChecker.isUnderOrAboveNonOwnedLockedChest(event.getPlayer(), event.getBlock());
            if (isNextToNonOwnedLockedChest || isUnderOrAboveNonOwnedLockedChest) {
                event.setCancelled(true);
                messageService.sendLocalizedMessage(player, "CannotPlaceHoppersNextToUnownedLockedChests");
            }
        }
    }

    @EventHandler()
    public void handle(PlayerInteractEvent event) {
        // Ignore events for offhand
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        
        Player player = event.getPlayer();

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (playerIsAttemptingToLockABlock(player)) {
            lockService.handleLockingBlock(event, player, clickedBlock);
        }

        if (playerIsAttemptingToUnlockABlock(player)) {
            lockService.handleUnlockingBlock(event, player, clickedBlock);
        }

        LockedBlock lockedBlock = this.dataService.getLockedBlock(clickedBlock);
        if (lockedBlock != null) {
            boolean playerHasAccess = lockedBlock.hasAccess(player.getUniqueId());
            boolean isPlayerBypassing = ephemeralData.getAdminsBypassingProtections().contains(player.getUniqueId());
            if (!playerHasAccess && !isPlayerBypassing) {
                UUIDChecker uuidChecker = new UUIDChecker();
                String owner = uuidChecker.findPlayerNameBasedOnUUID(lockedBlock.getOwner());
                messageService.sendLocalizedMessage(
                    player,
                    new MessageBuilder("LockedBy")
                        .with("name", owner)
                );
                event.setCancelled(true);
                return;
            }

            if (playerIsAttemptingToGrantAccess(player)) {
                lockService.handleGrantingAccess(event, clickedBlock, player);
            }

            if (playerIsAttemptingToCheckAccess(player)) {
                lockService.handleCheckingAccess(event, lockedBlock, player);
            }

            if (playerIsAttemptingToRevokeAccess(player)) {
                lockService.handleRevokingAccess(event, clickedBlock, player);
            }

            if (playerHasAccess) {
                /*
                Don't process any more checks so that the event is not cancelled
                when a player who is not part of the faction has access granted
                to a lock.
                */
                return;
            }

        } else {
            if (isPlayerUsingAnAccessCommand(player)) {
                messageService.sendLocalizedMessage(player, "BlockIsNotLocked");
            }
        }

        // Check if it's a lever, and if it is and it's connected to a gate in the faction territory then open/close the gate.
        boolean playerClickedLever = clickedBlock.getType().equals(Material.LEVER);
        if (playerClickedLever) {
            gateService.handlePotentialGateInteraction(clickedBlock, player, event);
        }

        // pgarner Sep 2, 2020: Moved this to after test to see if the block is locked because it could be a block they have been granted
        // access to (or in future, a 'public' locked block), so if they're not in the faction whose territory the block exists in we want that
        // check to be handled before the interaction is rejected for not being a faction member.
        ClaimedChunk chunk = this.dataService.getClaimedChunk(event.getClickedBlock().getLocation().getChunk());
        if (chunk != null) {
            persistentData.getChunkDataAccessor().handleClaimedChunkInteraction(event, chunk);
        }

        if (playerCreatingGate(player) && playerHoldingGoldenHoe(player)) {
            gateService.handleCreatingGate(clickedBlock, player, event);
        }
    }

    private boolean playerIsAttemptingToRevokeAccess(Player player) {
        return ephemeralData.getPlayersRevokingAccess().containsKey(player.getUniqueId());
    }

    private boolean playerHoldingGoldenHoe(Player player) {
        return player.getInventory().getItemInMainHand().getType().equals(Material.GOLDEN_HOE);
    }

    private boolean playerCreatingGate(Player player) {
        return ephemeralData.getCreatingGatePlayers().containsKey(player.getUniqueId());
    }

    private boolean playerIsAttemptingToCheckAccess(Player player) {
        return ephemeralData.getPlayersCheckingAccess().contains(player.getUniqueId());
    }

    private boolean playerIsAttemptingToGrantAccess(Player player) {
        return ephemeralData.getPlayersGrantingAccess().containsKey(player.getUniqueId());
    }

    private boolean playerIsAttemptingToUnlockABlock(Player player) {
        return ephemeralData.getUnlockingPlayers().contains(player.getUniqueId());
    }

    private boolean playerIsAttemptingToLockABlock(Player player) {
        return ephemeralData.getLockingPlayers().contains(player.getUniqueId());
    }

    @EventHandler()
    public void handle(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        Location location = null;

        if (clickedEntity instanceof ArmorStand) {
            ArmorStand armorStand = (ArmorStand) clickedEntity;

            // get chunk that armor stand is in
            location = armorStand.getLocation();
        } else if (clickedEntity instanceof ItemFrame) {
            if (factionsPlusPlus.isDebugEnabled()) {
                System.out.println("DEBUG: ItemFrame interaction captured in PlayerInteractAtEntityEvent!");
            }
            ItemFrame itemFrame = (ItemFrame) clickedEntity;

            // get chunk that armor stand is in
            location = itemFrame.getLocation();
        }

        if (location != null) {
            Chunk chunk = location.getChunk();
            ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(chunk);

            if (interactionAccessChecker.shouldEventBeCancelled(claimedChunk, player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler()
    public void handle(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getRemover();

        Entity entity = event.getEntity();

        // get chunk that entity is in
        ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(entity.getLocation().getChunk());

        if (interactionAccessChecker.shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerBucketFillEvent event) {
        if (factionsPlusPlus.isDebugEnabled()) {
            System.out.println("DEBUG: A player is attempting to fill a bucket!");
        }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlockClicked();

        ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(clickedBlock.getChunk());

        if (interactionAccessChecker.shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerBucketEmptyEvent event) {
        if (factionsPlusPlus.isDebugEnabled()) {
            System.out.println("DEBUG: A player is attempting to empty a bucket!");
        }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlockClicked();

        ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(clickedBlock.getChunk());

        if (interactionAccessChecker.shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(EntityPlaceEvent event) {
        if (factionsPlusPlus.isDebugEnabled()) {
            System.out.println("DEBUG: A player is attempting to place an entity!");
        }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlock();

        ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(clickedBlock.getChunk());

        if (interactionAccessChecker.shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        if (clickedEntity instanceof ItemFrame) {
            if (factionsPlusPlus.isDebugEnabled()) {
                System.out.println("DEBUG: ItemFrame interaction captured in PlayerInteractEntityEvent!");
            }
            ItemFrame itemFrame = (ItemFrame) clickedEntity;

            // get chunk that armor stand is in
            Location location = itemFrame.getLocation();
            Chunk chunk = location.getChunk();
            ClaimedChunk claimedChunk = this.dataService.getClaimedChunk(chunk);

            if (interactionAccessChecker.shouldEventBeCancelled(claimedChunk, player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler()
    public void handle(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        gateService.handlePotentialGateInteraction(block, event);
    }

    private boolean isPlayerUsingAnAccessCommand(Player player) {
        return ephemeralData.getPlayersGrantingAccess().containsKey(player.getUniqueId()) ||
                ephemeralData.getPlayersCheckingAccess().contains(player.getUniqueId()) ||
                ephemeralData.getPlayersRevokingAccess().containsKey(player.getUniqueId());
    }
}