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
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
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
            context.replyWith("CannotTransferToSelf");
            return;
        }

        if (context.getExecutorsFaction().isOfficer(targetUUID)) context.getExecutorsFaction().removeOfficer(targetUUID); // Remove Officer (if there is one)

        // set owner
        context.getExecutorsFaction().setOwner(targetUUID);
        context.replyWith(
            this.constructMessage("OwnerShipTransferredTo")
                .with("name", target.getName())
        );
        if (target.isOnline() && target.getPlayer() != null) { // Message if we can :)
            context.messagePlayer(
                target.getPlayer(),
                this.constructMessage("OwnershipTransferred")
                    .with("name", context.getExecutorsFaction().getName())
            );
        }
    }
}