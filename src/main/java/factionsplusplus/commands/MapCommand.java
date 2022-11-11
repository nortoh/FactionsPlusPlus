/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.commands.abs.FontMetrics;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.DataService;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;

import factionsplusplus.builders.CommandBuilder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
public class MapCommand extends Command {
    private final DataService dataService;

    @Inject
    public MapCommand(DataService dataService) {
        super(
            new CommandBuilder()
                .withName("map")
                .withAliases("showmap", "displaymap", LOCALE_PREFIX + "CmdMap")
                .withDescription("Display a map of the claims near your surroundings.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.map")
        );
        this.dataService = dataService;
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
        context.reply(FontMetrics.obtainCenteredMessage(String.format("&f%s", context.getLocalizedString("Generic.Compass.North"))));
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), task -> {
            List<String> mapLines = IntStream.rangeClosed(topLeftZ, bottomRightZ).boxed().map(zCoord -> {
                return IntStream.rangeClosed(topLeftX, bottomRightX).boxed().map(xCoord -> { // Represents a row
                    ClaimedChunk chunk = dataService.getClaimedChunk(xCoord, zCoord, context.getPlayer().getWorld().getUID());
                    boolean isPlayersChunk = center.getX() == xCoord && center.getZ() == zCoord && center.getWorld().equals(context.getPlayer().getWorld());
                    String color = "gray";
                    String key = "-";
                    String hoverText = "<color:green><lang:Generic.Unclaimed>";
                    // Chunk is claimed
                    if (chunk != null) {
                        String relation = "";
                        if (faction != null) {
                            color = "white";
                            if (chunk.getHolder().equals(faction.getUUID())) color = "green";
                            if (faction.isEnemy(chunk.getHolder())) {
                                color = "red";
                                relation = "Generic.Relation.Enemy";
                            }
                            if (faction.isAlly(chunk.getHolder())) {
                                color = "blue";
                                relation = "Generic.Relation.Ally";
                            }
                            if (faction.isVassal(chunk.getHolder())) {
                                color = "aqua";
                                relation = "Generic.Relation.Vassal";
                            }
                            if (faction.isLiege(chunk.getHolder())) {
                                color = "gold";
                                relation = "Generic.Relation.Liege";
                            }
                            if (relation.length() > 0) relation = String.format("(%s) ", context.getLocalizedString(relation));
                        }
                        key = "+";
                        hoverText = String.format("<color:yellow>%s", context.getLocalizedString("Generic.Claimed", String.format("%s%s", relation, dataService.getFaction(chunk.getHolder()).getName())));
                    }
                    // Is the players current location
                    if (isPlayersChunk) {
                        color = "light_purple";
                        hoverText = String.format("<color:gold><lang:Generic.YouAreHere>\n\n%s", hoverText);
                    }
                    return String.format("<hover:show_text:'%s'><color:%s>%s</color:%s></hover>", hoverText, color, key, color);
                }).collect(Collectors.joining(""));
            }).collect(Collectors.toList());
            mapLines.forEach(context::replyWithMiniMessage);
        });
    }
}