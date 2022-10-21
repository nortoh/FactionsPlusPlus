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
import factionsplusplus.models.InteractionContext;

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
        InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(context.getPlayer().getUniqueId());
        if (interactionContext == null) {
            this.ephemeralData.getPlayersPendingInteraction().put(
                context.getPlayer().getUniqueId(), 
                new InteractionContext(InteractionContext.Type.LockedBlockUnlock)
            );
            context.replyWith("RightClickUnlock");
        };
    }

    public void cancelCommand(CommandContext context) {
        InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(context.getPlayer().getUniqueId());
        if (interactionContext != null) {
            if (interactionContext.isLockedBlockUnlock() || interactionContext.isLockedBlockForceUnlock()) {
                this.ephemeralData.getPlayersPendingInteraction().remove(context.getPlayer().getUniqueId());
                context.replyWith("AlertUnlockingCancelled");
            }
        }
    }
}