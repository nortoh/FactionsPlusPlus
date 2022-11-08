/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.factories.LockedBlockFactory;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.models.InteractionContext;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.models.FPPPlayer;
import factionsplusplus.utils.BlockUtils;
import factionsplusplus.utils.BlockUtils.GenericBlockType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
    private final EphemeralData ephemeralData;
    private final DataService dataService;
    private final LockedBlockFactory lockedBlockFactory;
    private final LocaleService localeService;
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
    public LockService(EphemeralData ephemeralData, DataService dataService, LockedBlockFactory lockedBlockFactory, LocaleService localeService) {
        this.ephemeralData = ephemeralData;
        this.dataService = dataService;
        this.lockedBlockFactory = lockedBlockFactory;
        this.localeService = localeService;
    }

    public void handleLockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if chunk is claimed
        ClaimedChunk chunk = this.dataService.getClaimedChunk(Objects.requireNonNull(event.getClickedBlock()).getLocation().getChunk());
        FPPPlayer member = this.dataService.getPlayer(player.getUniqueId());
        if (chunk != null) {

            // if claimed by other faction
            if (! chunk.getHolder().equals(this.dataService.getPlayersFaction(player.getUniqueId()).getUUID())) {
                member.error("Error.Lock.ClaimedTerritory");
                event.setCancelled(true);
                return;
            }

            // if already locked
            if (this.dataService.isBlockLocked(clickedBlock)) {
                member.error("Error.Lock.AlreadyLocked");
                event.setCancelled(true);
                return;
            }

            // if the block is a lockable type
            if (! BlockUtils.isGenericBlockType(clickedBlock, LOCKABLE_BLOCKS)) {
                // TODO: localize block names using minecraft localization
                member.error("Error.Lock.CertainBlocks");
                event.setCancelled(true);
                return;
            }
            for (Block blockToLock : this.getAllRelatedBlocks(clickedBlock)) this.lockBlock(player, blockToLock);
            member.success("Generic.Locked");
            this.ephemeralData.getPlayersPendingInteraction().remove(player.getUniqueId());
        } else {
            member.error("Error.Lock.ClaimedTerritory");
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
        FPPPlayer member = this.dataService.getPlayer(player.getUniqueId());
        if (this.dataService.isBlockLocked(clickedBlock)) {
            if (
                this.dataService.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId()) ||
                context.isLockedBlockForceUnlock()
            ) {
                for (Block blockToUnlock : this.getAllRelatedBlocks(clickedBlock)) this.dataService.getLockedBlockRepository().delete(blockToUnlock);
                member.success("Generic.Unlocked");
                this.ephemeralData.getPlayersPendingInteraction().remove(player.getUniqueId());
                event.setCancelled(true);
            }
            return;
        }
        member.error("Error.Lock.NotLocked");
        event.setCancelled(true);
    }

    public void handleGrantingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {
        FPPPlayer member = this.dataService.getPlayer(player.getUniqueId());
        // if not owner
        if (! this.dataService.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())) {
            member.error("Error.Lock.NotOwner");
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
                grantedName = this.localeService.get("Generic.Ally.Plural").toLowerCase();
                break;
            case FactionMembers:
                lockedBlocks.forEach(b -> b.allowFactionMembers());
                grantedName = this.localeService.get("Generic.FactionMembers").toLowerCase();
                break;
        }
        member.success("CommandResponse.AccessGranted", grantedName);
        this.ephemeralData.getPlayersPendingInteraction().remove(player.getUniqueId());
        event.setCancelled(true);
    }

    public void handleCheckingAccess(PlayerInteractEvent event, LockedBlock lockedBlock, Player player) {
        FPPPlayer member = this.dataService.getPlayer(player.getUniqueId());
        member.alert(Component.translatable("AccessList.Lock.Title").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD));
        for (UUID playerUUID : lockedBlock.getAccessList()) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerUUID);
            member.alert("AccessList.Lock.Player", target.getName());
        }
        if (lockedBlock.shouldAllowAllies()) member.alert("AccessList.Lock.Allies");
        if (lockedBlock.shouldAllowFactionMembers()) {
            Faction playersFaction = this.dataService.getPlayersFaction(player);
            if (playersFaction != null) member.alert("AccessList.Lock.FactionMembers", playersFaction.getName());
        }
        this.ephemeralData.getPlayersPendingInteraction().remove(player.getUniqueId());
        event.setCancelled(true);
    }

    public void handleRevokingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {
        FPPPlayer member = this.dataService.getPlayer(player.getUniqueId());
        // if not owner
        if (! this.dataService.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())) {
            member.error("Error.Lock.NotOwner");
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
                revokedName = this.localeService.get("Generic.Ally.Plural").toLowerCase();
                break;
            case FactionMembers:
                lockedBlocks.forEach(b -> b.denyFactionMembers());
                revokedName = this.localeService.get("Generic.FactionMembers").toLowerCase();
                break;
        }

        member.success("CommandResponse.AccessRevoked", revokedName);

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