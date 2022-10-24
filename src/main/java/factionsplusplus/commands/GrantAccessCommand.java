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

import org.bukkit.OfflinePlayer;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.UUID;

/**
 * @author Callum Johnson
 */
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

    public void finalizeGrantRequest(CommandContext context, String target) {
        context.replyWith(
            this.constructMessage("RightClickGrantAccess")
                .with("name", target)
        );
    }

    public void playerCommand(CommandContext context) {
        if (!this.doCommonChecks(context)) return;
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID targetUUID = target.getUniqueId();
        if (targetUUID.equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotGrantAccessToSelf");
            return;
        }
        context.getPlayer().sendMessage(context.getPlayer().getUniqueId(),
        this.interactionContextFactory.create(
            InteractionContext.Type.LockedBlockGrant,
            InteractionContext.TargetType.Player,
            targetUUID
        ).toString());
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            this.interactionContextFactory.create(
                InteractionContext.Type.LockedBlockGrant,
                InteractionContext.TargetType.Player,
                targetUUID
            )
        );
        this.finalizeGrantRequest(context, target.getName());
    }

    public void alliesCommand(CommandContext context) {
        if (!this.doCommonChecks(context)) return;
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            this.interactionContextFactory.create(
                InteractionContext.Type.LockedBlockGrant,
                InteractionContext.TargetType.Allies
            )
        );
        this.finalizeGrantRequest(context, "all allied factions");
    }

    public void factionCommand(CommandContext context) {
        if (!this.doCommonChecks(context)) return;
        this.ephemeralData.getPlayersPendingInteraction().put(
            context.getPlayer().getUniqueId(),
            this.interactionContextFactory.create(
                InteractionContext.Type.LockedBlockGrant,
                InteractionContext.TargetType.FactionMembers
            )
        );
        this.finalizeGrantRequest(context, "all members of your faction");
    }

    public void cancelCommand(CommandContext context) {
        UUID playerUUID = context.getPlayer().getUniqueId();
        if (this.ephemeralData.getPlayersPendingInteraction().containsKey(playerUUID)) {
            InteractionContext interactionContext = this.ephemeralData.getPlayersPendingInteraction().get(playerUUID);
            if (interactionContext.isLockedBlockGrant()) {
                this.ephemeralData.getPlayersPendingInteraction().remove(playerUUID);
                context.replyWith("CommandCancelled");
            }
        }
    }
}