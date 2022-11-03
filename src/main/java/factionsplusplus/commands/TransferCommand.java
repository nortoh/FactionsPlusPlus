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
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.UUID;

@Singleton
public class TransferCommand extends Command {

    @Inject
    public TransferCommand() {
        super(
            new CommandBuilder()
                .withName("transfer")
                .withAliases(LOCALE_PREFIX + "CmdTransfer")
                .withDescription("Transfers ownership of your faction to another member.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.transfer")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the member to transfer ownership to")
                        .expectsFactionMember()
                        .addFilters(ArgumentFilterType.ExcludeSelf)
                        .isRequired()
                )
        );
    }

    public void execute(CommandContext context) {
        OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID targetUUID = target.getUniqueId();
        if (targetUUID.equals(context.getPlayer().getUniqueId())) {
            context.error("Error.TransferOwnership.Self");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                // Promote new owner
                context.getExecutorsFaction().upsertMember(targetUUID, GroupRole.Owner);
                // Demote old owner
                context.getExecutorsFaction().upsertMember(context.getPlayer().getUniqueId(), GroupRole.Member);
                // Notify
                context.success("CommandResponse.FactionOwnershipTransferred", context.getExecutorsFaction().getName(), target.getName());
                if (target.isOnline() && target.getPlayer() != null) { // Message if we can :)
                    context.alertPlayer(target, "PlayerNotice.FactionOwnershipTransferred", context.getExecutorsFaction().getName());
                }
            }
        });
    }
}