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

import org.bukkit.OfflinePlayer;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.UUID;

@Singleton
public class GrantAccessCommand extends Command {

    private final EphemeralData ephemeralData;
    private final InteractionContextFactory interactionContextFactory;

    @Inject
    public GrantAccessCommand(
        EphemeralData ephemeralData,
        InteractionContextFactory interactionContextFactory
    ) {
        super(
            new CommandBuilder()
                .withName("grantaccess")
                .withAliases("ga", LOCALE_PREFIX + "CmdGrantAccess")
                .withDescription("Grants access to a player for a locked block.")
                .requiresPermissions("mf.grantaccess")
                .expectsPlayerExecution()
                .requiresSubCommand()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("cancel")
                        .withAliases(LOCALE_PREFIX + "CmdGrantAccessCancel")
                        .withDescription("Cancels pending grant access request")
                        .setExecutorMethod("cancelCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("allies")
                        .withAliases(LOCALE_PREFIX + "CmdGrantAccessAllies")
                        .withDescription("Allow allies to access a locked block")
                        .setExecutorMethod("alliesCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("faction")
                        .withAliases(LOCALE_PREFIX + "CmdGrantAccessFaction")
                        .withDescription("Allow faction members to access a locked block")
                        .setExecutorMethod("factionCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("player")
                        .withAliases(LOCALE_PREFIX + "CmdGrantAccessPlayer")
                        .withDescription("Allow a player to access a locked block")
                        .setExecutorMethod("playerCommand")
                        .addArgument(
                            "player",
                            new ArgumentBuilder()
                                .setDescription("the player to grant access to")
                                .expectsAnyPlayer()
                                .addFilters(ArgumentFilterType.ExcludeSelf)
                                .isRequired()
                        )
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
                context.cancellableError("Error.Lock.AlreadyGranting", "/fpp grantaccess cancel");
                return false;
            }
            context.replyWith("Error.InteractionEvent.Replaced", interactionContext.toString());
        }
        return true;
    }

    public void finalizeRequest(CommandContext context, String target) {
        context.cancellable("CommandResponse.RightClick.GrantAccess", "/fpp grantaccess cancel", target);
    }

    public void playerCommand(CommandContext context) {
        if (! this.doCommonChecks(context)) return;
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID targetUUID = target.getUniqueId();
        if (targetUUID.equals(context.getPlayer().getUniqueId())) {
            context.error("Error.GrantAccess.Self");
            return;
        }
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            this.interactionContextFactory.create(
                InteractionContext.Type.LockedBlockGrant,
                InteractionContext.TargetType.Player,
                targetUUID
            )
        );
        this.finalizeRequest(context, target.getName());
    }

    public void alliesCommand(CommandContext context) {
        if (! this.doCommonChecks(context)) return;
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            this.interactionContextFactory.create(
                InteractionContext.Type.LockedBlockGrant,
                InteractionContext.TargetType.Allies
            )
        );
        this.finalizeRequest(context, context.getLocalizedString("Generic.Ally.Plural").toLowerCase());
    }

    public void factionCommand(CommandContext context) {
        if (! this.doCommonChecks(context)) return;
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            this.interactionContextFactory.create(
                InteractionContext.Type.LockedBlockGrant,
                InteractionContext.TargetType.FactionMembers
            )
        );
        this.finalizeRequest(context, context.getLocalizedString("Generic.FactionMembers").toLowerCase());
    }

    public void cancelCommand(CommandContext context) {
        UUID playerUUID = context.getPlayer().getUniqueId();
        if (this.ephemeralData.getPlayersPendingInteraction().containsKey(playerUUID)) {
            InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(playerUUID);
            if (interactionContext.isLockedBlockGrant()) {
                this.ephemeralData.getPlayersPendingInteraction().remove(playerUUID);
                context.success("CommandResponse.RightClick.Cancelled");
            }
        }
    }
}