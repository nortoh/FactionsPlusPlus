/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.services.ClaimService;
import factionsplusplus.services.DynmapIntegrationService;
import factionsplusplus.builders.*;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class ClaimCommand extends Command {

    private final DynmapIntegrationService dynmapService;
    private final ClaimService claimService;

    @Inject
    public ClaimCommand(ClaimService claimService, DynmapIntegrationService dynmapService) {
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
                        .isOptional()
                )
        );
        this.claimService = claimService;
        this.dynmapService = dynmapService;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        if (context.getExecutorsFaction().getFlag("mustBeOfficerToManageLand").toBoolean()) {
            // officer or owner rank required
            if (! context.getExecutorsFaction().isOfficer(player.getUniqueId()) && ! context.getExecutorsFaction().isOwner(player.getUniqueId())) {
                context.replyWith("AlertMustBeOfficerOrOwnerToClaimLand");
                return;
            }
        }

        Integer depth = context.getIntegerArgument("radius");
        if (depth != null) {
            if (depth <= 0) {
                context.replyWith("UsageClaimRadius");
            } else {
                this.claimService.radiusClaimAtLocation(depth, player, player.getLocation(), context.getExecutorsFaction());
            }
        } else {
            this.claimService.claimChunkAtLocation(player, player.getLocation(), context.getExecutorsFaction());
        }
        this.dynmapService.updateClaimsIfAble();
    }
}