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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class PromoteCommand extends Command {

    @Inject
    public PromoteCommand() {
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
    }

    public void execute(CommandContext context) {
        final Faction faction = context.getExecutorsFaction();
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        if (faction.isOfficer(target.getUniqueId())) {
            context.error("Error.PlayerAlreadyRole", context.getLocalizedString("Generic.Role.Officer"));
            return;
        }
        if (context.getPlayer().getUniqueId().equals(target.getUniqueId())) {
            context.error("Error.Promote.Self");
            return;
        }
        if (faction.getOfficerCount() <= faction.calculateMaxOfficers()) {
            Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), task -> {
                faction.upsertMember(target.getUniqueId(), GroupRole.Officer);
                context.success("CommandResponse.Member.Promoted", target.getName());
                if (target.isOnline() && target.getPlayer() != null) {
                    context.alertPlayer(target, "PlayerNotice.Promoted", context.getLocalizedString("Generic.Role.Officer"));
                }
            });
            return;
        }
        context.error("Error.OfficerLimit");
    }
}