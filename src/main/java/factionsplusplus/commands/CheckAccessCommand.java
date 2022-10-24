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

import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class CheckAccessCommand extends Command {

    private final EphemeralData ephemeralData;
    private final InteractionContextFactory interactionContextFactory;

    @Inject
    public CheckAccessCommand(
        EphemeralData ephemeralData,
        InteractionContextFactory interactionContextFactory
    ) {
        super(
            new CommandBuilder()
                .withName("checkaccess")
                .withAliases("ca", LOCALE_PREFIX + "CmdCheckAccess")
                .withDescription("Checks access to a locked block.")
                .requiresPermissions("mf.checkaccess")
                .expectsPlayerExecution()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("cancel")
                        .withAliases(LOCALE_PREFIX + "CmdCheckAccessCancel")
                        .withDescription("Cancels pending check access request")
                        .setExecutorMethod("cancelCommand")
                )
        );
        this.ephemeralData = ephemeralData;
        this.interactionContextFactory = interactionContextFactory;
    }

    public boolean doCommonChecks(CommandContext context) {
        UUID playerUUID = context.getPlayer().getUniqueId();
        if (this.ephemeralData.getPlayersPendingInteraction().containsKey(playerUUID)) {
            InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(playerUUID);
            if (interactionContext.isLockedBlockGrant()) {
                context.replyWith("AlertAlreadyGrantingAccess");
                return false;
            }
            context.replyWith(
                this.constructMessage("CancelInteraction")
                    .with("type", interactionContext.toString())
            );
        }
        return true;
    }

    public void execute(CommandContext context) {
        UUID playerUUID = context.getPlayer().getUniqueId();
        if (this.ephemeralData.getPlayersPendingInteraction().containsKey(playerUUID)) {
            InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(playerUUID);
            if (interactionContext.isLockedBlockInquiry()) {
                context.replyWith("AlreadyEnteredCheckAccess");
                return;
            }
            context.replyWith(
                this.constructMessage("CancelInteraction")
                    .with("type", interactionContext.toString())
            );
            this.ephemeralData.getPlayersPendingInteraction().remove(playerUUID);
        }

        this.ephemeralData.getPlayersPendingInteraction().put(
            playerUUID,
            this.interactionContextFactory.create(InteractionContext.Type.LockedBlockInquiry)
        );
        context.replyWith("RightClickCheckAccess");
    }

    public void cancelCommand(CommandContext context) {
        UUID playerUUID = context.getPlayer().getUniqueId();
        if (this.ephemeralData.getPlayersPendingInteraction().containsKey(playerUUID)) {
            InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(playerUUID);
            if (interactionContext.isLockedBlockInquiry()) {
                this.ephemeralData.getPlayersPendingInteraction().remove(playerUUID);
                context.replyWith("Cancelled");
            }
        }
    }
}