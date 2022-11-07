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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import factionsplusplus.builders.CommandBuilder;

import java.util.Collection;

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
            context.replyWith("CommandResponse.Faction.NoneFound");
            return;
        }
        context.getExecutorsAudience().sendMessage(
            Component.translatable("FactionList.Title").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD)
        );
        Collection<Faction> sortedFactionList = this.factionService.getFactionsByPower();
        context.replyWith("FactionList.Legend");
        for (Faction faction : sortedFactionList) {
            context.getExecutorsAudience().sendMessage(
                Component.translatable("FactionList.Faction").args(Component.text(faction.getName())).color(NamedTextColor.AQUA)
                    .hoverEvent(HoverEvent.showText(
                        Component.text()
                            .append(Component.text("Power: ").decorate(TextDecoration.BOLD)) // TODO: localize
                            .append(Component.text(faction.getCumulativePowerLevel()+"\n"))
                            .append(Component.text("Members: ").decorate(TextDecoration.BOLD)) // TODO: localize
                            .append(Component.text(faction.getMemberCount()+"\n"))
                            .append(Component.text("Land: ").decorate(TextDecoration.BOLD)) // TODO: localize
                            .append(Component.text(this.dataService.getClaimedChunksForFaction(faction).size()))
                    ))
            );
        }
    }
}