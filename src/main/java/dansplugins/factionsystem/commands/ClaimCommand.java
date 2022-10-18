/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.services.DynmapIntegrationService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.builders.*;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class ClaimCommand extends Command {

    private final PersistentData persistentData;
    private final DynmapIntegrationService dynmapService;

    @Inject
    public ClaimCommand(PersistentData persistentData, DynmapIntegrationService dynmapService) {
        super(
            new CommandBuilder()
                .withName("claim")
                .withAliases(LOCALE_PREFIX + "CmdClaim")
                .withDescription("Claim land for your faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.claim")
                .expectsFactionMembership()
                .addArgument(
                    "radius",
                    new ArgumentBuilder()
                        .setDescription("the chunk radius to claim")
                        .expectsInteger()
                )
        );
        this.persistentData = persistentData;
        this.dynmapService = dynmapService;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        if (context.getExecutorsFaction().getFlag("mustBeOfficerToManageLand").toBoolean()) {
            // officer or owner rank required
            if (!context.getExecutorsFaction().isOfficer(player.getUniqueId()) && !context.getExecutorsFaction().isOwner(player.getUniqueId())) {
                context.replyWith("AlertMustBeOfficerOrOwnerToClaimLand");
                return;
            }
        }

        Object depthArgument = context.getArgument("radius");
        if (depthArgument != null) {
            int depth = (int)depthArgument;
            if (depth <= 0) {
                context.replyWith("UsageClaimRadius");
            } else {
                this.persistentData.getChunkDataAccessor().radiusClaimAtLocation(depth, player, player.getLocation(), context.getExecutorsFaction());
            }
        } else {
            this.persistentData.getChunkDataAccessor().claimChunkAtLocation(player, player.getLocation(), context.getExecutorsFaction());
        }
        this.dynmapService.updateClaimsIfAble();
    }
}