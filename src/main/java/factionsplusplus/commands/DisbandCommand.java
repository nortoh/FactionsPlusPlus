/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.repositories.FactionRepository;
import factionsplusplus.events.internal.FactionDisbandEvent;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.utils.Logger;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;
import org.bukkit.Bukkit;

@Singleton
public class DisbandCommand extends Command {

    private final EphemeralData ephemeralData;
    private final Logger logger;
    private final FactionRepository factionRepository;

    @Inject
    public DisbandCommand(
        Logger logger,
        EphemeralData ephemeralData,
        FactionRepository factionRepository
    ) {
        super(
            new CommandBuilder()
                .withName("disband")
                .withAliases(LOCALE_PREFIX + "CmdDisband")
                .withDescription("Disband your faction (must be owner).")
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to disband")
                        .expectsFaction()
                        .consumesAllLaterArguments()
                        .requiresPermissionsIfNull("mf.disband")
                        .requiresPermissionsIfNotNull("mf.disband.others", "mf.admin")
                        .isOptional()
                )
        );
        this.logger = logger;
        this.ephemeralData = ephemeralData;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        Faction disband = null;
        final boolean self;
        if (context.getRawArguments().length == 0) {
            if (context.isConsole()) {
                context.error("Error.PlayerExecutionRequired");
                return;
            }
            disband = context.getExecutorsFaction();
            self = true;
            if (disband != null && disband.getMemberCount() != 1) {
                context.error("Error.Faction.DisbandHasMembers");
                return;
            }
        } else {
            disband = context.getFactionArgument("faction name");
            self = false;
        }
        if (disband == null) {
            context.error("Error.Faction.NotFound", String.join(" ", context.getRawArguments()));
            return;
        }
        final Faction targetFaction = disband;
        // Check if anybody wants us to not disband
        FactionDisbandEvent event = new FactionDisbandEvent(
            targetFaction,
            context.getPlayer()
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.logger.debug("Disband event was cancelled.");
            context.error("Error.Faction.DisbandError");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), task -> {
            factionRepository.delete(targetFaction);
            if (self) ephemeralData.getPlayersInFactionChat().remove(context.getPlayer().getUniqueId());
            context.success("CommandResponse.Faction.Disbanded", targetFaction.getName());
        });
    }
}