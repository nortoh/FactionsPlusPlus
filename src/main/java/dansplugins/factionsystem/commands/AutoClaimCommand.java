/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.builders.CommandBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class AutoClaimCommand extends Command {

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public AutoClaimCommand() {
        super(
            new CommandBuilder()
                .withName("autoclaim")
                .withAliases("ac", LOCALE_PREFIX + "CmdAutoClaim")
                .withDescription("Toggles auto claim for faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.autoclaim")
                .expectsFactionMembership()
                .expectsFactionOwnership()
        );
    }

    public void execute(CommandContext context) {
        context.getExecutorsFaction().toggleAutoClaim();
        context.replyWith("AutoclaimToggled");
    }
}