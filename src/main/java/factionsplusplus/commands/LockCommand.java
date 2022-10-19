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
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;

import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class LockCommand extends Command {

    private final EphemeralData ephemeralData;

    @Inject
    public LockCommand(EphemeralData ephemeralData) {
        super(
            new CommandBuilder()
                .withName("lock")
                .withAliases(LOCALE_PREFIX + "CmdLock")
                .withDescription("Lock a chest or door.")
                .requiresPermissions("mf.lock")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("cancel")
                        .withAliases(LOCALE_PREFIX + "CmdLockCancel")
                        .withDescription("cancels a pending lock request")
                        .setExecutorMethod("cancelCommand")
                )
        );
        this.ephemeralData = ephemeralData;
    }

    public void execute(CommandContext context) {
        // TODO: handle if already locking?
        this.ephemeralData.getLockingPlayers().add(context.getPlayer().getUniqueId());
        this.ephemeralData.getUnlockingPlayers().remove(context.getPlayer().getUniqueId());
        context.replyWith("RightClickLock");
    }

    public void cancelCommand(CommandContext context) {
        if (this.ephemeralData.getLockingPlayers().remove(context.getPlayer().getUniqueId())) { // Remove them
            context.replyWith("LockingCancelled");
        }
        // TODO: handle if no active locking?
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        return TabCompleteTools.completeSingleOption(args[0], "cancel");
    }
}