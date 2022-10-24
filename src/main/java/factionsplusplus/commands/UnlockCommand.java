/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.factories.InteractionContextFactory;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.InteractionContext;

import factionsplusplus.builders.CommandBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class UnlockCommand extends Command {

    private final EphemeralData ephemeralData;
    private final InteractionContextFactory interactionContextFactory;

    @Inject
    public UnlockCommand(
        EphemeralData ephemeralData,
        InteractionContextFactory interactionContextFactory
    ) {
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
        this.interactionContextFactory = interactionContextFactory;
    }

    public void execute(CommandContext context) {
        InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(context.getPlayer().getUniqueId());
        if (interactionContext == null) {
            this.ephemeralData.getPlayersPendingInteraction().put(
                context.getPlayer().getUniqueId(),
                this.interactionContextFactory.create(InteractionContext.Type.LockedBlockUnlock)
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