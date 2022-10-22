package factionsplusplus.utils;

import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Gate;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Set;
import java.util.HashSet;

public class ChunkUtils {
    public static boolean isGateInChunk(Gate gate, ClaimedChunk chunk) {
        return (
            (gate.getTopLeftChunkX() == chunk.getCoordinates()[0] || gate.getBottomRightChunkX() == chunk.getCoordinates()[0]) &&
            (gate.getTopLeftChunkZ() == chunk.getCoordinates()[1] || gate.getBottomRightChunkZ() == chunk.getCoordinates()[1])
        );
    }

    public static Set<Chunk> obtainChunksForRadius(Chunk initial, int radius) {
        final Set<Chunk> chunkSet = new HashSet<>(); // Avoid duplicates without checking for it yourself.
        for (int x = initial.getX() - radius; x <= initial.getX() + radius; x++) {
            for (int z = initial.getZ() - radius; z <= initial.getZ() + radius; z++) {
                chunkSet.add(initial.getWorld().getChunkAt(x, z));
            }
        }
        return chunkSet;
    }

    public static double[] getChunkCoords(Chunk chunk) {
        double[] chunkCoords = new double[2];
        chunkCoords[0] = chunk.getX();
        chunkCoords[1] = chunk.getZ();
        return chunkCoords;
    }

    public static double[] getChunkCoords(Location location) {
        return ChunkUtils.getChunkCoords(location.getChunk());
    }

    /**
    * This can be utilized to get a chunk locationally relative to another chunk.
     *
     * @param origin    The chunk we are checking.
     * @param direction The direction the chunk we want to grab is.
     */
    public static Chunk getChunkByDirection(Chunk origin, String direction) {
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
}