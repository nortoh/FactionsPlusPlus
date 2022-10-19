/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.utils.TabCompleteTools;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.List;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class RevokeAccessCommand extends Command {

    private final EphemeralData ephemeralData;

    @Inject
    public RevokeAccessCommand(EphemeralData ephemeralData) {
        super(
            new CommandBuilder()
                .withName("revokeaccess")
                .withAliases("ra", LOCALE_PREFIX + "CmdRevokeAccess")
                .withDescription("Revokes access from a player for a locked block.")
                .requiresPermissions("mf.revokeaccess")
                .expectsPlayerExecution()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("cancel")
                        .withAliases(LOCALE_PREFIX + "CmdRevokeAccessCancel")
                        .withDescription("Cancels pending revoke access request")
                        .setExecutorMethod("cancelCommand")
                )
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the player to revoke access from")
                        .expectsAnyPlayer()
                        .isRequired()
                )
        );
        this.ephemeralData = ephemeralData;
    }


    public void execute(CommandContext context) {
        if (this.ephemeralData.getPlayersRevokingAccess().containsKey(context.getPlayer().getUniqueId())) {
            context.replyWith("AlreadyEnteredRevokeAccess");
            return;
        }
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID targetUUID = target.getUniqueId();
        if (targetUUID.equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotRevokeAccessFromSelf");
            return;
        }
        this.ephemeralData.getPlayersRevokingAccess().put(
                context.getPlayer().getUniqueId(), targetUUID
        );
        context.replyWith("RightClickRevokeAccess");
    }

    public void cancelCommand(CommandContext context) {
        this.ephemeralData.getPlayersRevokingAccess().remove(context.getPlayer().getUniqueId());
        context.replyWith("Cancelled");
    }


    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        return TabCompleteTools.allOnlinePlayersMatching(args[0], true);
    }
}