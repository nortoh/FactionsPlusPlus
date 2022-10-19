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
public class GrantAccessCommand extends Command {

    private final EphemeralData ephemeralData;

    @Inject
    public GrantAccessCommand(EphemeralData ephemeralData) {
        super(
            new CommandBuilder()
                .withName("grantaccess")
                .withAliases("ga", LOCALE_PREFIX + "CmdGrantAccess")
                .withDescription("Grants access to a player for a locked block.")
                .requiresPermissions("mf.grantaccess")
                .expectsPlayerExecution()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("cancel")
                        .withAliases(LOCALE_PREFIX + "CmdGrantAccessCancel")
                        .withDescription("Cancels pending grant access request")
                        .setExecutorMethod("cancelCommand")
                )
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the player to grant access to")
                        .expectsAnyPlayer()
                        .isRequired()
                )
        );
        this.ephemeralData = ephemeralData;
    }

    public void execute(CommandContext context) {
        if (this.ephemeralData.getPlayersGrantingAccess().containsKey(context.getPlayer().getUniqueId())) {
            context.replyWith("AlertAlreadyGrantingAccess");
            return;
        }
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID targetUUID = target.getUniqueId();
        if (targetUUID.equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotGrantAccessToSelf");
            return;
        }
        this.ephemeralData.getPlayersGrantingAccess().put(context.getPlayer().getUniqueId(), targetUUID);
        context.replyWith(
            this.constructMessage("RightClickGrantAccess")
                .with("name", target.getName())
        );
    }

    public void cancelCommand(CommandContext context) {
        if (this.ephemeralData.getPlayersGrantingAccess().containsKey(context.getPlayer().getUniqueId())) {
            this.ephemeralData.getPlayersGrantingAccess().remove(context.getPlayer().getUniqueId());
            context.replyWith("CommandCancelled");
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
        return TabCompleteTools.allOnlinePlayersMatching(args[0]);
    }
}