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
import factionsplusplus.builders.CommandBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class CheckClaimCommand extends Command {

    private final ClaimService claimService;

    @Inject
    public CheckClaimCommand(ClaimService claimService) {
        super(
            new CommandBuilder()
                .withName("checkclaim")
                .withAliases("cc", LOCALE_PREFIX + "CmdCheckClaim")
                .withDescription("Check if land is claimed.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.checkclaim")
        );
        this.claimService = claimService;
    }

    public void execute(CommandContext context) {
        final String result = this.claimService.checkOwnershipAtPlayerLocation(context.getPlayer());

        if (result.equals("unclaimed")) {
            context.replyWith("LandIsUnclaimed");
        } else {
            context.replyWith(
                this.constructMessage("LandClaimedBy")
                    .with("player", result)
            );
        }
    }
}