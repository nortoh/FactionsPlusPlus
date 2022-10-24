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

import org.bukkit.OfflinePlayer;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.builders.ArgumentBuilder;

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
                .requiresSubCommand()
                .addSubCommand(
                    new CommandBuilder()
                        .withName("cancel")
                        .withAliases(LOCALE_PREFIX + "CmdRevokeAccessCancel")
                        .withDescription("Cancels pending revoke access request")
                        .setExecutorMethod("cancelCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("allies")
                        .withAliases(LOCALE_PREFIX + "CmdRevokeAccessAllies")
                        .withDescription("Revokes access to a locked block from allies")
                        .setExecutorMethod("alliesCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("faction")
                        .withAliases(LOCALE_PREFIX + "CmdRevokeAccessFaction")
                        .withDescription("Revokes access to a locked block from faction members")
                        .setExecutorMethod("factionCommand")
                )
                .addSubCommand(
                    new CommandBuilder()
                        .withName("player")
                        .withAliases(LOCALE_PREFIX + "CmdRevokeAccessPlayer")
                        .withDescription("Revokes access to a locked block from a player")
                        .setExecutorMethod("playerCommand")
                        .addArgument(
                            "player",
                            new ArgumentBuilder()
                                .setDescription("the player to revoke access from")
                                .expectsAnyPlayer()
                                .addFilters(ArgumentFilterType.ExcludeSelf)
                                .isRequired()
                        )
                )
        );
        this.ephemeralData = ephemeralData;
    }


    public boolean doCommonChecks(CommandContext context) {
        UUID playerUUID = context.getPlayer().getUniqueId();
        if (this.ephemeralData.getPlayersPendingInteraction().containsKey(playerUUID)) {
            InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(playerUUID);
            if (interactionContext.isLockedBlockRevoke()) {
                context.replyWith("AlreadyEnteredRevokeAccess");
                return false;
            }
            context.replyWith(
                this.constructMessage("CancelInteraction")
                    .with("type", interactionContext.toString())
            );
        }
        return true;
    }

    public void finalizeRequest(CommandContext context, String target) {
        context.replyWith(
            this.constructMessage("RightClickRevokeAccess")
                .with("name", target)
        );
    }

    public void playerCommand(CommandContext context) {
        if (!this.doCommonChecks(context)) return;
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID targetUUID = target.getUniqueId();
        if (targetUUID.equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotRevokeAccessFromSelf");
            return;
        }
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            new InteractionContext(
                InteractionContext.Type.LockedBlockRevoke,
                InteractionContext.TargetType.Player,
                targetUUID
            )
        );
        this.finalizeRequest(context, target.getName());
    }

    public void alliesCommand(CommandContext context) {
        if (!this.doCommonChecks(context)) return;
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            new InteractionContext(
                InteractionContext.Type.LockedBlockRevoke,
                InteractionContext.TargetType.Allies
            )
        );
        this.finalizeRequest(context, "all allied factions");
    }

    public void factionCommand(CommandContext context) {
        if (!this.doCommonChecks(context)) return;
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            new InteractionContext(
                InteractionContext.Type.LockedBlockRevoke,
                InteractionContext.TargetType.FactionMembers
            )
        );
        this.finalizeRequest(context, "all members of your faction");
    }

    public void cancelCommand(CommandContext context) {
        UUID playerUUID = context.getPlayer().getUniqueId();
        if (this.ephemeralData.getPlayersPendingInteraction().containsKey(playerUUID)) {
            InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(playerUUID);
            if (interactionContext.isLockedBlockRevoke()) {
                this.ephemeralData.getPlayersPendingInteraction().remove(playerUUID);
                context.replyWith("Cancelled");
            }
        }
    }
}