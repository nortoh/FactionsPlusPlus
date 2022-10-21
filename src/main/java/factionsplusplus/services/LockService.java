/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.utils.BlockUtils;
import factionsplusplus.utils.BlockUtils.GenericBlockType;
import factionsplusplus.builders.MessageBuilder;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class LockService {
    private final MessageService messageService;
    private final EphemeralData ephemeralData;
    private final DataService dataService;
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
    public LockService(MessageService messageService, EphemeralData ephemeralData, DataService dataService) {
        this.messageService = messageService;
        this.ephemeralData = ephemeralData;
        this.dataService = dataService;
    }

    public void handleLockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if chunk is claimed
        ClaimedChunk chunk = this.dataService.getClaimedChunk(Objects.requireNonNull(event.getClickedBlock()).getLocation().getChunk());
        if (chunk != null) {

            // if claimed by other faction
            if (!chunk.getHolder().equals(this.dataService.getPlayersFaction(player.getUniqueId()).getID())) {
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
            if (!BlockUtils.isGenericBlockType(clickedBlock, LOCKABLE_BLOCKS)) {
                this.messageService.sendLocalizedMessage(player, "CanOnlyLockSpecificBlocks");
                event.setCancelled(true);
                return;
            }
            for (Block blockToLock : this.getAllRelatedBlocks(clickedBlock)) this.lockBlock(player, blockToLock);
            this.messageService.sendLocalizedMessage(player, "Locked");
            this.ephemeralData.getLockingPlayers().remove(player.getUniqueId());
        } else {
            this.messageService.sendLocalizedMessage(player, "CanOnlyLockBlocksInClaimedTerritory");
        }
        event.setCancelled(true);
    }

    public void lockBlock(Player player, Block block) {
        this.dataService.getLockedBlockRepository().create(
            new LockedBlock(
                player.getUniqueId(),
                this.dataService.getPlayersFaction(player.getUniqueId()).getID(),
                block.getX(),
                block.getY(),
                block.getZ(),
                block.getWorld().getName()
            )
        );
    }

    public void handleUnlockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if locked
        if (this.dataService.isBlockLocked(clickedBlock)) {
            if (
                this.dataService.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId()) || 
                this.ephemeralData.getForcefullyUnlockingPlayers().contains(player.getUniqueId())
            ) {
                for (Block blockToUnlock : this.getAllRelatedBlocks(clickedBlock)) this.dataService.getLockedBlockRepository().delete(blockToUnlock);
                this.messageService.sendLocalizedMessage(player, "Unlocked");
                this.ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());

                // remove player from forcefully unlocking players list if they are in it
                this.ephemeralData.getForcefullyUnlockingPlayers().remove(player.getUniqueId());

                event.setCancelled(true);
            }
        } else {
            this.messageService.sendLocalizedMessage(player, "BlockIsNotLocked");
            event.setCancelled(true);
        }
    }

    public void handleGrantingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {
        // if not owner
        if (! this.dataService.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())) {
            this.messageService.sendLocalizedMessage(player, "NotTheOwnerOfThisBlock");
            return;
        }

        final UUID targetUUID = this.ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId());
        for (Block block : this.getAllRelatedBlocks(clickedBlock)) this.dataService.getLockedBlock(block).addToAccessList(targetUUID);
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

        this.messageService.sendLocalizedMessage(
            player,
            new MessageBuilder("AlertAccessGrantedTo")
                .with("name", target.getName())
        );

        this.ephemeralData.getPlayersGrantingAccess().remove(player.getUniqueId());

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
        this.ephemeralData.getPlayersCheckingAccess().remove(player.getUniqueId());
        event.setCancelled(true);
    }

    public void handleRevokingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {
        // if not owner
        if (! this.dataService.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())) {
            this.messageService.sendLocalizedMessage(player, "NotTheOwnerOfThisBlock");
            return;
        }

        UUID targetUUID = this.ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId());
        for (Block block : this.getAllRelatedBlocks(clickedBlock)) this.dataService.getLockedBlock(block).removeFromAccessList(targetUUID);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

        this.messageService.sendLocalizedMessage(
            player,
            new MessageBuilder("AlertAccessRevokedFor")
                .with("name", target.getName())
        );

        this.ephemeralData.getPlayersRevokingAccess().remove(player.getUniqueId());

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