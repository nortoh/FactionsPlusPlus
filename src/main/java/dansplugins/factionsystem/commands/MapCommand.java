/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.FontMetrics;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.ClaimedChunk;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import org.bukkit.Chunk;

import dansplugins.factionsystem.builders.CommandBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class MapCommand extends Command {
    private final char[] map_keys = "\\/#$%=&^ABCDEFGHJKLMNOPQRSTUVWXYZ0123456789abcdeghjmnopqrsuvwxyz?".toCharArray();

    private final PersistentData persistentData;

    @Inject
    public MapCommand(PersistentData persistentData) {
        super(
            new CommandBuilder()
                .withName("map")
                .withAliases("showmap", "displaymap", LOCALE_PREFIX + "CmdMap")
                .withDescription("Display a map of the claims near your surroundings.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.map")
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        final Chunk center = context.getPlayer().getLocation().getChunk();
        // Needs to be Odd.
        int map_width = 53;
        final int topLeftX = center.getX() - (map_width / 2);
        // Needs to be Odd.
        int map_height = 13;
        final int topLeftZ = center.getZ() - (map_height / 2);
        final int bottomRightX = center.getX() + (map_width / 2);
        final int bottomRightZ = center.getZ() + (map_height / 2);
        final Faction faction = context.getExecutorsFaction();
        final boolean hasFaction = faction != null;
        final HashMap<String, Integer> printedHolders = new HashMap<>();
        final HashMap<String, String> colourMap = new HashMap<>();
        context.reply(FontMetrics.obtainCenteredMessage("&fNorth"));
        for (int z = topLeftZ; z <= bottomRightZ; z++) {
            final StringBuilder line = new StringBuilder();
            for (int x = topLeftX; x <= bottomRightX; x++) {
                Chunk tmp = center.getWorld().getChunkAt(x, z);
                if (this.persistentData.getChunkDataAccessor().isClaimed(tmp)) {
                    ClaimedChunk chunk = this.persistentData.getChunkDataAccessor().getClaimedChunk(tmp);
                    Faction chunkHolder = this.persistentData.getFactionByID(chunk.getHolder());
                    printedHolders.put(chunkHolder.getName(), printedHolders.getOrDefault(chunkHolder.getName(), 0) + 1);
                    int index = this.getIndex(chunkHolder.getName(), printedHolders);
                    char map_key = index == -1 ? '§' : map_keys[index];
                    if (hasFaction) {
                        String colour;
                        if (chunk.getChunk().equals(center)) {
                            colour = "&5"; // If the current position is the player-position, make it purple.
                            map_key = '+';
                            printedHolders.put(chunkHolder.getName(), printedHolders.get(chunkHolder.getName()) - 1);
                        } else if (chunkHolder.getName().equals(faction.getName())) {
                            colour = "&a"; // If the faction is the player-faction, make it green.
                            map_key = '+';
                        } else if (faction.isEnemy(chunk.getHolder())) {
                            colour = "&c"; // If they are an enemy to the player-faction, make it red.
                            colourMap.put(chunkHolder.getName(), "&c");
                        } else if (faction.isAlly(chunk.getHolder())) {
                            colour = "&b"; // If they are an ally to the player-faction, make it blue.
                            colourMap.put(chunkHolder.getName(), "&b");
                        } else {
                            colour = "&f"; // Default to White.
                            colourMap.put(chunkHolder.getName(), "&f");
                        }
                        line.append(colour);
                    } else {
                        line.append("&c"); // Always default to Enemy.
                    }
                    line.append(map_key);
                } else {
                    if (tmp.equals(center)) {
                        line.append("&5+"); // If the current position is the player-position, make it purple.
                    } else {
                        line.append("&7-"); // Gray for no Faction.
                    }
                }
            }
            context.reply(this.translate(line.toString()));
        }
        context.reply(this.translate(" &5+&7 = You"));
        final List<String> added = new ArrayList<>();
        int index = 0;
        for (String printedHolder : printedHolders.keySet()) {
            if (!(printedHolders.get(printedHolder) <= 0)) {
                String line;
                try {
                    if (hasFaction && printedHolder.equalsIgnoreCase(faction.getName())) {
                        line = "&a+&7 = " + printedHolder;
                    } else {
                        if (hasFaction) {
                            line = colourMap.get(printedHolder) + map_keys[index] + "&7 = " + printedHolder;
                        } else {
                            line = "&c" + map_keys[index] + "&7 = " + printedHolder;
                        }
                    }
                } catch (IndexOutOfBoundsException ex) {
                    line = "&7§ = " + printedHolder;
                }
                added.add(line);
            }
            index++;
        }
        if (!added.isEmpty()) { // We don't wanna send an empty line, so check if the added lines is empty or not.
            context.reply(" " + this.translate(String.join(", ", added)));
        }
    }

    /**
     * Method to obtain the index of a key in a hashmap.
     *
     * @param holder         or key.
     * @param printedHolders hashmap.
     * @return integer index or {@code -1}.
     */
    private int getIndex(String holder, HashMap<String, Integer> printedHolders) {
        return new ArrayList<>(printedHolders.keySet()).indexOf(holder);
    }
}