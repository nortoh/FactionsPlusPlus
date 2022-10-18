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
import dansplugins.factionsystem.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class DescCommand extends Command {

    @Inject
    public DescCommand() {
        super(
            new CommandBuilder()
                .withName("description")
                .withAliases("desc", LOCALE_PREFIX + "CmdDesc")
                .withDescription("Set your faction description.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.desc")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "description",
                    new ArgumentBuilder()
                        .setDescription("the description to set")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                        
                )
        );
    }

    public void execute(CommandContext context) {
        String description = context.getStringArgument("description");
        context.getExecutorsFaction().setDescription(description);
        context.replyWith(
            this.constructMessage("DescriptionSet")
                .with("desc", description)
        );
    }
}