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
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.FactionService;
import factionsplusplus.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class UnclaimallCommand extends Command {

    private PersistentData persistentData;
    private FactionRepository factionRepository;
    private FactionService factionService;

    @Inject
    public UnclaimallCommand(
        PersistentData persistentData,
        FactionService factionService,
        FactionRepository factionRepository
    ) {
        super(
            new CommandBuilder()
                .withName("unclaimall")
                .withAliases("ua", LOCALE_PREFIX + "CmdUnclaimall")
                .withDescription("Unclaims all land from your faction (must be owner).")
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to unclaim all land from")
                        .expectsFaction()
                        .consumesAllLaterArguments()
                        .requiresPermissionsIfNull("mf.unclaimall")
                        .requiresPermissionsIfNotNull("mf.unclaimall.others", "mf.admin")
                )
        );
        this.persistentData = persistentData;
        this.factionService = factionService;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        final Faction faction;
        if (context.getRawArguments().length == 0) {
            // Self
            if (context.isConsole()) {
                context.replyWith("OnlyPlayersCanUseCommand");
                return;
            }
            faction = context.getExecutorsFaction();
            if (faction == null) {
                context.replyWith("AlertMustBeInFactionToUseCommand");
                return;
            }
            if (!faction.isOwner(context.getPlayer().getUniqueId())) {
                context.replyWith("AlertMustBeOwnerToUseCommand");
                return;
            }
        } else {
            faction = context.getFactionArgument("faction name");
        }
        // remove faction home
        faction.setFactionHome(null);
        context.messageFaction(faction, "AlertFactionHomeRemoved");

        // remove claimed chunks
        this.factionService.unclaimAllClaimedChunks(faction);
        context.replyWith(
            this.constructMessage("AllLandUnclaimedFrom")
                .with("name", faction.getName())
        );

        // remove locks associated with this faction
        this.factionService.removeAllOwnedLocks(faction);
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