/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionDisbandEvent;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.DynmapIntegrationService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TabCompleteTools;
import dansplugins.factionsystem.builders.*;
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
    private final DynmapIntegrationService dynmapService;
    private final Logger logger;
    private final FactionRepository factionRepository;

    @Inject
    public DisbandCommand(
        Logger logger,
        PersistentData persistentData,
        DynmapIntegrationService dynmapService,
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
                )
        );
        this.logger = logger;
        this.persistentData = persistentData;
        this.dynmapService = dynmapService;
        this.ephemeralData = ephemeralData;
        this.factionRepository = factionRepository;
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

        // remove claimed land objects associated with this faction
        this.persistentData.getChunkDataAccessor().removeAllClaimedChunks(faction.getID());
        this.dynmapService.updateClaimsIfAble();

        // remove locks associated with this faction
        this.persistentData.removeAllLocks(faction.getID());

        this.persistentData.removePoliticalTiesToFaction(faction);

        this.factionRepository.delete(faction);
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