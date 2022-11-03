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
import factionsplusplus.services.FactionService;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class UnclaimallCommand extends Command {

    private FactionService factionService;

    @Inject
    public UnclaimallCommand(FactionService factionService) {
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
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        final Faction faction;
        if (context.getRawArguments().length == 0) {
            // Self
            if (context.isConsole()) {
                context.error("Error.PlayerExecutionRequired");
                return;
            }
            faction = context.getExecutorsFaction();
            if (faction == null) {
                context.error("Error.Faction.MembershipNeeded");
                return;
            }
            if (! faction.isOwner(context.getPlayer().getUniqueId())) {
                context.error("Error.Faction.OwnershipNeeded");
                return;
            }
        } else {
            faction = context.getFactionArgument("faction name");
        }
        // remove faction bases
        this.factionService.removeAllBases(faction);
        // remove claimed chunks
        this.factionService.unclaimAllClaimedChunks(faction);
        // remove locks associated with this faction
        this.factionService.removeAllOwnedLocks(faction);
        context.success("CommandResponse.AllLandUnclaimed", faction.getName());
    }
}