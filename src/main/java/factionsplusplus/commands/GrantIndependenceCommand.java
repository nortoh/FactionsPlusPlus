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
import factionsplusplus.repositories.FactionRepository;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.Objects;
import java.util.List;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class GrantIndependenceCommand extends Command {

    private final FactionRepository factionRepository;

    @Inject
    public GrantIndependenceCommand(FactionRepository factionRepository) {
        super(
            new CommandBuilder()
                .withName("grantindependence")
                .withAliases("gi", LOCALE_PREFIX + "CmdGrandIndependence")
                .withDescription("Grants independence to a vassaled faction.")
                .requiresPermissions("mf.grantindependence")
                .expectsPlayerExecution()
                .expectsNoFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to get a members list of")
                        .expectsVassaledFaction()
                        .consumesAllLaterArguments()
                        .isRequired()
                )
        );
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        final Faction target = context.getFactionArgument("faction name");
        target.setLiege(null);
        context.getExecutorsFaction().removeVassal(target.getID());
        
        // inform all players in that faction that they are now independent
        context.messageFaction(
            target,
            this.constructMessage("AlertGrantedIndependence")
                .with("name", context.getExecutorsFaction().getName())
        );
        // inform all players in players faction that a vassal was granted independence
        context.messagePlayersFaction(
            this.constructMessage("AlertNoLongerVassalFaction")
                .with("name", target.getName())
        );
    }
}