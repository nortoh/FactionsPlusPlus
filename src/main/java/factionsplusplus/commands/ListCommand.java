/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.DataService;
import factionsplusplus.services.FactionService;
import factionsplusplus.builders.CommandBuilder;
import org.bukkit.ChatColor;

import java.util.Collection;
import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class ListCommand extends Command {

    private final FactionService factionService;
    private final DataService dataService;

    @Inject
    public ListCommand(
        FactionService factionService,
        DataService dataService
    ) {
        super(
            new CommandBuilder()
                .withName("list")
                .withAliases(LOCALE_PREFIX + "CmdList")
                .withDescription("List all factions on the server.")
                .requiresPermissions("mf.list")
        );
        this.factionService = factionService;
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        if (this.dataService.getNumberOfFactions() == 0) {
            context.replyWith("CurrentlyNoFactions");
            return;
        }
        context.replyWith("FactionsTitle");
        Collection<Faction> sortedFactionList = this.factionService.getFactionsByPower();
        context.replyWith("ListLegend");
        context.reply(ChatColor.AQUA + "-----");
        for (Faction faction : sortedFactionList) {

            context.reply(ChatColor.AQUA + String.format("%-25s %10s %10s %10s", faction.getName(), "P: " +
                    this.factionService.getCumulativePowerLevel(faction), "M: " + faction.getPopulation(), "L: " +
                    this.dataService.getClaimedChunksForFaction(faction).size()));
        }
    }
}