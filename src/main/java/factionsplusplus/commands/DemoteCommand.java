/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class DemoteCommand extends Command {

    @Inject
    public DemoteCommand() {
        super(
            new CommandBuilder()
                .withName("demote")
                .withAliases(LOCALE_PREFIX + "CmdDemote")
                .withDescription("Demote an officer of your faction.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.demote")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the officer to demote")
                        .expectsFactionOfficer()
                        .isRequired()
                )
        );
    }

    public void execute(CommandContext context) {
        OfflinePlayer playerToBeDemoted = context.getOfflinePlayerArgument("player");

        if (playerToBeDemoted.getUniqueId().equals(context.getPlayer().getUniqueId())) {
            context.error("Error.Demote.Self");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                context.getExecutorsFaction().upsertMember(playerToBeDemoted.getUniqueId(), GroupRole.Member);
                if (playerToBeDemoted.isOnline()) {
                    context.alertPlayer(playerToBeDemoted, "PlayerNotice.Demoted", context.getLocalizedString("Generic.Role.Member"));
                }
                context.success("CommandResponse.Member.Demoted", playerToBeDemoted.getName());
            }
        });
    }
}