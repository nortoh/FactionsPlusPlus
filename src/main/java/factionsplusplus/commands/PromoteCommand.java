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
import org.bukkit.OfflinePlayer;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class PromoteCommand extends Command {

    private final FactionService factionService;
    private final DataService dataService;

    @Inject
    public PromoteCommand(FactionService factionService, DataService dataService) {
        super(
            new CommandBuilder()
                .withName("promote")
                .withAliases(LOCALE_PREFIX + "CmdPromote")
                .withDescription("Promotes a member of your faction to an officer.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.promote")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the member to promote")
                        .expectsFactionMember()
                        .addFilters(ArgumentFilterType.ExcludeOfficers, ArgumentFilterType.ExcludeSelf)
                        .isRequired()
                )
        );
        this.factionService = factionService;
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        final Faction faction = context.getExecutorsFaction();
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        if (faction.isOfficer(target.getUniqueId())) {
            context.replyWith("PlayerAlreadyOfficer");
            return;
        }
        if (context.getPlayer().getUniqueId().equals(target.getUniqueId())) {
            context.replyWith("CannotPromoteSelf");
            return;
        }
        int maxOfficers = this.factionService.calculateMaxOfficers(faction);
        if (faction.getOfficerCount() <= maxOfficers) {
            this.dataService.updatePlayersFactionRole(faction, target, GroupRole.Officer);
            context.replyWith("PlayerPromoted");
            if (target.isOnline() && target.getPlayer() != null) {
                context.messagePlayer(target.getPlayer(), "PromotedToOfficer");
            }
        } else {
            context.replyWith(
                this.constructMessage("PlayerCantBePromotedBecauseOfLimit")
                    .with("number", String.valueOf(maxOfficers))
            );
        }
    }
}