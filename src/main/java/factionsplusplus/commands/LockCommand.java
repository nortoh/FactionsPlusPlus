/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.data.factories.InteractionContextFactory;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.InteractionContext;
import factionsplusplus.builders.CommandBuilder;

@Singleton
public class LockCommand extends Command {

    private final EphemeralData ephemeralData;
    private final InteractionContextFactory interactionContextFactory;

    @Inject
    public LockCommand(
        EphemeralData ephemeralData,
        InteractionContextFactory interactionContextFactory
    ) {
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
        this.interactionContextFactory = interactionContextFactory;
    }

    public void execute(CommandContext context) {
        InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(context.getPlayer().getUniqueId());
        if (interactionContext != null) {
            if (interactionContext.isLockedBlockLock()) {
                context.cancellableError("Error.Lock.AlreadyLocking", "/fpp lock cancel");
                return;
            }
            context.replyWith("Error.InteractionEvent.Replaced", interactionContext.toString());
        }
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            this.interactionContextFactory.create(InteractionContext.Type.LockedBlockLock)
        );
        context.cancellable("CommandResponse.RightClick.Lock", "/fpp lock cancel");
    }

    public void cancelCommand(CommandContext context) {
        InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(context.getPlayer().getUniqueId());
        if (interactionContext != null) {
            if (interactionContext.isLockedBlockLock()) {
                this.ephemeralData.getPlayersPendingInteraction().remove(context.getPlayer().getUniqueId());
                context.success("CommandResponse.RightClick.Cancelled");
            }
        }
    }
}