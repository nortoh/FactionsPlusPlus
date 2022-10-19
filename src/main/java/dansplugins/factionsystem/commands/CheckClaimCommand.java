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
import dansplugins.factionsystem.builders.CommandBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class CheckClaimCommand extends Command {

    private final PersistentData persistentData;

    @Inject
    public CheckClaimCommand(PersistentData persistentData) {
        super(
            new CommandBuilder()
                .withName("checkclaim")
                .withAliases("cc", LOCALE_PREFIX + "CmdCheckClaim")
                .withDescription("Check if land is claimed.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.checkclaim")
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        final String result = this.persistentData.getChunkDataAccessor().checkOwnershipAtPlayerLocation(context.getPlayer());

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