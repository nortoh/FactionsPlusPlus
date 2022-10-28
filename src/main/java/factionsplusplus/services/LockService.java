/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.factories.LockedBlockFactory;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.InteractionContext;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.utils.BlockUtils;
import factionsplusplus.utils.BlockUtils.GenericBlockType;
import factionsplusplus.builders.MessageBuilder;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class LockService {
    private final MessageService messageService;
    private final EphemeralData ephemeralData;
    private final DataService dataService;
    private final LockedBlockFactory lockedBlockFactory;
    private final static GenericBlockType[] LOCKABLE_BLOCKS = {
      GenericBlockType.Door,
      GenericBlockType.Chest,
      GenericBlockType.Gate,
      GenericBlockType.Barrel,
      GenericBlockType.TrapDoor,
      GenericBlockType.Furance,
      GenericBlockType.Anvil  
    };

    @Inject
    public LockService(MessageService messageService, EphemeralData ephemeralData, DataService dataService, LockedBlockFactory lockedBlockFactory) {
        this.messageService = messageService;
        this.ephemeralData = ephemeralData;
        this.dataService = dataService;
        this.lockedBlockFactory = lockedBlockFactory;
    }

    public void handleLockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if chunk is claimed
        ClaimedChunk chunk = this.dataService.getClaimedChunk(Objects.requireNonNull(event.getClickedBlock()).getLocation().getChunk());
        if (chunk != null) {

            // if claimed by other faction
            if (! chunk.getHolder().equals(this.dataService.getPlayersFaction(player.getUniqueId()).getID())) {
                this.messageService.sendLocalizedMessage(player, "CanOnlyLockInFactionTerritory");
                event.setCancelled(true);
                return;
            }

            // if already locked
            if (this.dataService.isBlockLocked(clickedBlock)) {
                this.messageService.sendLocalizedMessage(player, "BlockAlreadyLocked");
                event.setCancelled(true);
                return;
            }

            // if the block is a lockable type
            if (! BlockUtils.isGenericBlockType(clickedBlock, LOCKABLE_BLOCKS)) {
                this.messageService.sendLocalizedMessage(player, "CanOnlyLockSpecificBlocks");
                event.setCancelled(true);
                return;
            }
            for (Block blockToLock : this.getAllRelatedBlocks(clickedBlock)) this.lockBlock(player, blockToLock);
            this.messageService.sendLocalizedMessage(player, "Locked");
            this.ephemeralData.getPlayersPendingInteraction().remove(player.getUniqueId());
        } else {
            this.messageService.sendLocalizedMessage(player, "CanOnlyLockBlocksInClaimedTerritory");
        }
        event.setCancelled(true);
    }

    public void lockBlock(Player player, Block block) {
        this.dataService.getLockedBlockRepository().create(
            this.lockedBlockFactory.create(
                player.getUniqueId(),
                this.dataService.getPlayersFaction(player.getUniqueId()).getUUID(),
                block.getX(),
                block.getY(),
                block.getZ(),
                block.getWorld().getUID()
            )
        );
    }

    public void handleUnlockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if locked
        InteractionContext context = this.ephemeralData.getPlayersPendingInteraction().get(player.getUniqueId());
        if (context == null) return;
        if (this.dataService.isBlockLocked(clickedBlock)) {
            if (
                this.dataService.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId()) ||
                context.isLockedBlockForceUnlock()
            ) {
                for (Block blockToUnlock : this.getAllRelatedBlocks(clickedBlock)) this.dataService.getLockedBlockRepository().delete(blockToUnlock);
                this.messageService.sendLocalizedMessage(player, "Unlocked");
                this.ephemeralData.getPlayersPendingInteraction().remove(player.getUniqueId());
                event.setCancelled(true);
            }
            return;
        }
        this.messageService.sendLocalizedMessage(player, "BlockIsNotLocked");
        event.setCancelled(true);
    }

    public void handleGrantingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {
        // if not owner
        if (! this.dataService.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())) {
            this.messageService.sendLocalizedMessage(player, "NotTheOwnerOfThisBlock");
            return;
        }
        InteractionContext context = this.ephemeralData.getPlayersPendingInteraction().get(player.getUniqueId());
        if (context == null) return;
        String grantedName = null;
        List<LockedBlock> lockedBlocks = this.getAllRelatedBlocks(clickedBlock).stream().map(b -> this.dataService.getLockedBlock(b)).collect(Collectors.toList());
        switch(context.getTargetType()) {
            case Player:
                lockedBlocks.forEach(b -> b.addToAccessList(context.getUUID()));
                grantedName = Bukkit.getOfflinePlayer(context.getUUID()).getName();
                break;
            case Allies:
                lockedBlocks.forEach(b -> b.allowAllies());
                grantedName = "all allied factions";
                break;
            case FactionMembers:
                lockedBlocks.forEach(b -> b.allowFactionMembers());
                grantedName = "all members of your faction";
                break;
        }
        this.messageService.sendLocalizedMessage(
            player,
            new MessageBuilder("AlertAccessGrantedTo")
                .with("name", grantedName)
        );
        this.ephemeralData.getPlayersPendingInteraction().remove(player.getUniqueId());
        event.setCancelled(true);
    }

    public void handleCheckingAccess(PlayerInteractEvent event, LockedBlock lockedBlock, Player player) {
        this.messageService.sendLocalizedMessage(player, "FollowingPlayersHaveAccess");
        for (UUID playerUUID : lockedBlock.getAccessList()) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerUUID);
            this.messageService.sendLocalizedMessage(
                player, 
                new MessageBuilder("FPHAList")
                    .with("name", target.getName())
            );
        }
        if (lockedBlock.shouldAllowAllies()) this.messageService.sendLocalizedMessage(player, "FPHAAllies");
        if (lockedBlock.shouldAllowFactionMembers()) this.messageService.sendLocalizedMessage(player, "FPHAMembers");
        this.ephemeralData.getPlayersPendingInteraction().remove(player.getUniqueId());
        event.setCancelled(true);
    }

    public void handleRevokingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {
        // if not owner
        if (! this.dataService.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())) {
            this.messageService.sendLocalizedMessage(player, "NotTheOwnerOfThisBlock");
            return;
        }

        InteractionContext context = this.ephemeralData.getPlayersPendingInteraction().get(player.getUniqueId());
        if (context == null) return;
        String revokedName = null;
        List<LockedBlock> lockedBlocks = this.getAllRelatedBlocks(clickedBlock).stream().map(b -> this.dataService.getLockedBlock(b)).collect(Collectors.toList());
        switch(context.getTargetType()) {
            case Player:
                lockedBlocks.forEach(b -> b.removeFromAccessList(context.getUUID()));
                revokedName = Bukkit.getOfflinePlayer(context.getUUID()).getName();
                break;
            case Allies:
                lockedBlocks.forEach(b -> b.denyAllies());
                revokedName = "all allied factions";
                break;
            case FactionMembers:
                lockedBlocks.forEach(b -> b.denyFactionMembers());
                revokedName = "all members of your faction";
                break;
        }

        this.messageService.sendLocalizedMessage(
            player,
            new MessageBuilder("AlertAccessRevokedFor")
                .with("name", revokedName)
        );

        this.ephemeralData.getPlayersPendingInteraction().remove(player.getUniqueId());

        event.setCancelled(true);
    }

    private ArrayList<Block> getAllRelatedBlocks(Block block) {
        GenericBlockType blockType = BlockUtils.toGenericType(block);
        ArrayList<Block> relatedBlocks = new ArrayList<>();
        switch(blockType) {
            case Chest:
                if (BlockUtils.isDoubleChest(block)) {
                    relatedBlocks.addAll(Arrays.asList(BlockUtils.getDoubleChestSides(block)));
                } else relatedBlocks.add(block);
                break;
            case Door:
                relatedBlocks.addAll(Arrays.asList(BlockUtils.getDoorBlocks(block)));
            default:
                relatedBlocks.add(block);
                break;
        }
        return relatedBlocks;
    }
}