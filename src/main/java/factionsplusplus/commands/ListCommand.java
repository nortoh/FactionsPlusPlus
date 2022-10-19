/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.FactionService;
import factionsplusplus.builders.CommandBuilder;
import org.bukkit.ChatColor;

import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class ListCommand extends Command {

    private final PersistentData persistentData;
    private final FactionService factionService;

    @Inject
    public ListCommand(
        PersistentData persistentData,
        FactionService factionService
    ) {
        super(
            new CommandBuilder()
                .withName("list")
                .withAliases(LOCALE_PREFIX + "CmdList")
                .withDescription("List all factions on the server.")
                .requiresPermissions("mf.list")
        );
        this.persistentData = persistentData;
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        if (this.persistentData.getNumFactions() == 0) {
            context.replyWith("CurrentlyNoFactions");
            return;
        }
        context.replyWith("FactionsTitle");
        List<PersistentData.SortableFaction> sortedFactionList = this.persistentData.getSortedListOfFactions();
        context.replyWith("ListLegend");
        context.reply(ChatColor.AQUA + "-----");
        for (PersistentData.SortableFaction sortableFaction : sortedFactionList) {
            final Faction temp = sortableFaction.getFaction();
            context.reply(ChatColor.AQUA + String.format("%-25s %10s %10s %10s", temp.getName(), "P: " +
                    this.factionService.getCumulativePowerLevel(temp), "M: " + temp.getPopulation(), "L: " +
                    this.persistentData.getChunkDataAccessor().getChunksClaimedByFaction(temp.getID())));
        }
    }
}