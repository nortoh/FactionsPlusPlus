/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.utils.extended;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.services.DataService;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Caibinus
 */
@Singleton
public class BlockChecker extends preponderous.ponder.minecraft.bukkit.tools.BlockChecker {
    @Inject private DataService dataService;

    public boolean isNextToNonOwnedLockedChest(Player player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
        Block neighbor3 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
        Block neighbor4 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);

        if (this.isChest(neighbor1)) {
            if (this.dataService.isBlockLocked(neighbor1) && this.dataService.getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (this.isChest(neighbor2)) {
            if (this.dataService.isBlockLocked(neighbor2) && this.dataService.getLockedBlock(neighbor2).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (this.isChest(neighbor3)) {
            if (this.dataService.isBlockLocked(neighbor3) && this.dataService.getLockedBlock(neighbor3).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (this.isChest(neighbor4)) {
            return this.dataService.isBlockLocked(neighbor4) && this.dataService.getLockedBlock(neighbor4).getOwner() != player.getUniqueId();
        }

        return false;
    }

    public boolean isUnderOrAboveNonOwnedLockedChest(Player player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());

        if (this.isChest(neighbor1)) {
            if (this.dataService.isBlockLocked(neighbor1) && this.dataService.getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (this.isChest(neighbor2)) {
            return this.dataService.isBlockLocked(neighbor2) && this.dataService.getLockedBlock(neighbor2).getOwner() != player.getUniqueId();
        }

        return false;
    }
}