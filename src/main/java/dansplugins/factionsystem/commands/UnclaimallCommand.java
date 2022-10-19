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
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.DynmapIntegrationService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class UnclaimallCommand extends Command {

    private PersistentData persistentData;
    private MessageService messageService;
    private DynmapIntegrationService dynmapService;
    private FactionRepository factionRepository;

    @Inject
    public UnclaimallCommand(
        PersistentData persistentData,
        MessageService messageService,
        DynmapIntegrationService dynmapService,
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
        this.messageService = messageService;
        this.dynmapService = dynmapService;
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
        this.messageService.sendFactionLocalizedMessage(faction, "AlertFactionHomeRemoved");

        // remove claimed chunks
        this.persistentData.getChunkDataAccessor().removeAllClaimedChunks(faction.getID());
        this.dynmapService.updateClaimsIfAble();
        context.replyWith(
            this.constructMessage("AllLandUnclaimedFrom")
                .with("name", faction.getName())
        );

        // remove locks associated with this faction
        this.persistentData.removeAllLocks(faction.getID());
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