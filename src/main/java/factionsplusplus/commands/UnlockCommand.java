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
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;

import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class UnlockCommand extends Command {

    private final EphemeralData ephemeralData;

    @Inject
    public UnlockCommand(EphemeralData ephemeralData) {
        super(
            new CommandBuilder()
                .withName("unlock")
                .withAliases(LOCALE_PREFIX + "CmdUnlock")
                .withDescription("Unlock a chest or door.")
                .requiresPermissions("mf.unlock")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("cancel")
                        .withAliases(LOCALE_PREFIX + "CmdUnlockCancel")
                        .withDescription("cancels a pending unlock request")
                        .setExecutorMethod("cancelCommand")
                )
        );
        this.ephemeralData = ephemeralData;
    }

    public void execute(CommandContext context) {
        if (!this.ephemeralData.getUnlockingPlayers().contains(context.getPlayer().getUniqueId())) {
            this.ephemeralData.getUnlockingPlayers().add(context.getPlayer().getUniqueId());
        }
        this.ephemeralData.getLockingPlayers().remove(context.getPlayer().getUniqueId());

        // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
        context.replyWith("RightClickUnlock");
    }

    public void cancelCommand(CommandContext context) {
        this.ephemeralData.getUnlockingPlayers().remove(context.getPlayer().getUniqueId());
        this.ephemeralData.getForcefullyUnlockingPlayers().remove(context.getPlayer().getUniqueId()); // just in case the player tries to cancel a forceful unlock without using the force command
        context.replyWith("AlertUnlockingCancelled");
    }
}