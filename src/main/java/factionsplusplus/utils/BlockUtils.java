package factionsplusplus.utils;


import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.*;
import org.bukkit.block.data.Bisected;

import java.util.AbstractMap;
import java.util.Arrays;
import java.lang.Record;

public class BlockUtils {
    public static enum GenericBlockType {
        Chest,
        Gate,
        Door,
        Furance,
        Anvil,
        TrapDoor,
        Barrel,
        Bed,
        Fence,
        Wall,
        Stairs,
        Slab,
        Switch,
        Unknown
    }

    public static GenericBlockType toGenericType(Block block) {
        BlockData blockData = block.getBlockData();
        String materialName = block.getType().name();
        if (blockData instanceof TrapDoor) return GenericBlockType.TrapDoor;
        if (blockData instanceof Door) return GenericBlockType.Door;
        if (blockData instanceof Barrel) return GenericBlockType.Barrel;
        if (blockData instanceof Gate) return GenericBlockType.Gate;
        if (blockData instanceof Furnace) return GenericBlockType.Furance;
        if (blockData instanceof Chest) return GenericBlockType.Chest;
        if (blockData instanceof Bed) return GenericBlockType.Bed;
        if (blockData instanceof Fence) return GenericBlockType.Fence;
        if (blockData instanceof Wall) return GenericBlockType.Wall;
        if (blockData instanceof Stairs) return GenericBlockType.Stairs;
        if (blockData instanceof Slab) return GenericBlockType.Slab;
        if (blockData instanceof Switch) return GenericBlockType.Switch;
        if (materialName.contains("ANVIL")) return GenericBlockType.Anvil;
        return GenericBlockType.Unknown;
    }

    public static Boolean isGenericBlockType(Block block, BlockUtils.GenericBlockType[] types) {
        return Arrays.asList(types).contains(BlockUtils.toGenericType(block));
    }

    // TODO: probably throw an error here if something that isn't a chest is sent here
    public static Boolean isDoubleChest(Block block) {
        Chest chestData = (Chest)block.getBlockData();
        return ! chestData.getType().equals(Chest.Type.SINGLE);
    }

    // TODO: probably throw an error here if something that isn't a chest is sent here
    public static Block[] getDoubleChestSides(Block block) {
        org.bukkit.block.DoubleChest doubleChest = (org.bukkit.block.DoubleChest)((org.bukkit.block.Chest) block.getState()).getInventory().getHolder();
        return new Block[]{
            ((org.bukkit.block.Chest) doubleChest.getLeftSide()).getBlock(),
            ((org.bukkit.block.Chest) doubleChest.getRightSide()).getBlock()
        };
    }

    // TODO: probably throw an error here if something that isn't a door is sent here
    public static Block[] getDoorBlocks(Block block) {
        Door doorData = (Door)block.getBlockData();
        Block[] blocks = new Block[]{
            block,
            null
        };
        if (doorData.getHalf() == Bisected.Half.TOP) blocks[1] = BlockUtils.getBlockBelow(block);
        blocks[1] = BlockUtils.getBlockAbove(block);
        return blocks;
    }

    public static Block getBlockAbove(Block block) {
        return block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
    }

    public static Block getBlockBelow(Block block) {
        return block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());
    }
}