/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.FactionService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.builders.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class ListCommand extends Command {

    private final PlayerService playerService;
    private final LocaleService localeService;
    private final PersistentData persistentData;
    private final FactionService factionService;

    @Inject
    public ListCommand(
        PlayerService playerService,
        LocaleService localeService,
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
        this.playerService = playerService;
        this.localeService = localeService;
        this.persistentData = persistentData;
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        CommandSender sender = context.getSender();
        if (this.persistentData.getNumFactions() == 0) {
            this.playerService.sendMessage(
                sender, 
                "&b" + this.localeService.getText("CurrentlyNoFactions"),
                "CurrentlyNoFactions", 
                false
            );
            return;
        }
        this.playerService.sendMessage(
            sender, 
            "&b&l" + this.localeService.getText("FactionsTitle"),
            "FactionsTitle", 
            false
        );
        List<PersistentData.SortableFaction> sortedFactionList = this.persistentData.getSortedListOfFactions();
        sender.sendMessage(ChatColor.AQUA + this.localeService.get("ListLegend"));
        sender.sendMessage(ChatColor.AQUA + "-----");
        for (PersistentData.SortableFaction sortableFaction : sortedFactionList) {
            final Faction temp = sortableFaction.getFaction();
            sender.sendMessage(ChatColor.AQUA + String.format("%-25s %10s %10s %10s", temp.getName(), "P: " +
                    this.factionService.getCumulativePowerLevel(temp), "M: " + temp.getPopulation(), "L: " +
                    this.persistentData.getChunkDataAccessor().getChunksClaimedByFaction(temp.getName())));
        }
    }
}