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
import factionsplusplus.models.Faction;
import factionsplusplus.services.ClaimService;
import factionsplusplus.services.DynmapIntegrationService;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class UnclaimCommand extends Command {

    private final EphemeralData ephemeralData;
    private final DynmapIntegrationService dynmapService;
    private final ClaimService claimService;

    @Inject
    public UnclaimCommand(
        EphemeralData ephemeralData,
        DynmapIntegrationService dynmapService,
        ClaimService claimService
    ) {
        super(
            new CommandBuilder()
                .withName("unclaim")
                .withAliases(LOCALE_PREFIX + "CmdUnclaim")
                .withDescription("Unclaims land for your faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.unclaim")
                .expectsFactionMembership()
                .addArgument(
                    "radius",
                    new ArgumentBuilder()
                        .setDescription("the chunk radius to unclaim")
                        .expectsInteger()
                        .isOptional()
                )
        );
        this.ephemeralData = ephemeralData;
        this.dynmapService = dynmapService;
        this.claimService = claimService;
    }

    public void execute(CommandContext context) {
        final Faction faction = context.getExecutorsFaction();
        final Player player = context.getPlayer();
        final boolean isPlayerBypassing = this.ephemeralData.getAdminsBypassingProtections().contains(player.getUniqueId());
        if (faction.getFlag("mustBeOfficerToManageLand").toBoolean()) {
            // officer or owner rank required
            if (! faction.isOfficer(player.getUniqueId()) && ! faction.isOwner(player.getUniqueId()) && ! isPlayerBypassing) {
                context.replyWith("NotAbleToClaim");
                return;
            }
        }
        if (context.getRawArguments().length == 0) {
            this.claimService.removeChunkAtPlayerLocation(player, faction);
            this.dynmapService.updateClaimsIfAble();
            // Claim Service currently handles the message that would be sent if the chunk was removed.
            // TODO: Also, we don't check if the user currently has a claim??
            return;
        }
        int radius = context.getIntegerArgument("radius");
        if (radius <= 0) {
            radius = 1;
        }
        this.claimService.radiusUnclaimAtLocation(radius, player, faction);
        context.replyWith(
            this.constructMessage("UnClaimedRadius")
                .with("number", String.valueOf(radius))
        );
    }
}