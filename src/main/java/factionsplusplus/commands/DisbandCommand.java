/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.PersistentData;
import factionsplusplus.events.FactionDisbandEvent;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.FactionService;
import factionsplusplus.utils.Logger;
import factionsplusplus.utils.TabCompleteTools;
import factionsplusplus.builders.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DisbandCommand extends Command {

    private final PersistentData persistentData;
    private final EphemeralData ephemeralData;
    private final Logger logger;
    private final FactionRepository factionRepository;
    private final FactionService factionService;

    @Inject
    public DisbandCommand(
        Logger logger,
        PersistentData persistentData,
        EphemeralData ephemeralData,
        FactionRepository factionRepository,
        FactionService factionService
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
                )
        );
        this.logger = logger;
        this.persistentData = persistentData;
        this.ephemeralData = ephemeralData;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        Faction disband = null;
        final boolean self;
        final CommandSender sender = context.getSender();
        if (context.getRawArguments().length == 0) {
            if (context.isConsole()) {
                context.replyWith("OnlyPlayersCanUseCommand");
                return;
            }
            disband = context.getExecutorsFaction();
            self = true;
            if (disband != null && disband.getPopulation() != 1) {
                context.replyWith("AlertMustKickAllPlayers");
                return;
            }
        } else {
            disband = context.getFactionArgument("faction name");
            self = false;
        }
        if (disband == null) {
            context.replyWith(
                this.constructMessage("FactionNotFound")
                    .with("faction", String.join(" ", context.getRawArguments()))
            );
            return;
        }
        if (self) {
            context.replyWith("FactionSuccessfullyDisbanded");
            this.ephemeralData.getPlayersInFactionChat().remove(context.getPlayer().getUniqueId());
        } else {
            context.replyWith(
                this.constructMessage("SuccessfulDisbandment")
                    .with("faction", disband.getName())
            );
        }
        this.removeFaction(disband, self ? ((OfflinePlayer) sender) : null);
    }

    private void removeFaction(Faction faction, OfflinePlayer disbandingPlayer) {
        String nameOfFactionToRemove = faction.getName();
        FactionDisbandEvent event = new FactionDisbandEvent(
                faction,
                disbandingPlayer
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.logger.debug("Disband event was cancelled.");
            return;
        }
        this.factionService.removeFaction(faction);
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}