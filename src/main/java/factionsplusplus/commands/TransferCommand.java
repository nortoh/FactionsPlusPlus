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
import factionsplusplus.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
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

    private final PersistentData persistentData;

    @Inject
    public TransferCommand(PersistentData persistentData) {
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
                        .isRequired()
                )
        );
        this.persistentData = persistentData;
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

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        final List<String> membersInFaction = new ArrayList<>();
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            for (UUID uuid : playerFaction.getMemberList()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    membersInFaction.add(member.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], membersInFaction);
        }
        return null;
    }
}