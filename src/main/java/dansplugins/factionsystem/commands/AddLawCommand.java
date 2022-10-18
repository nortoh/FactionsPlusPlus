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
public class AddLawCommand extends Command {

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public AddLawCommand() {
        super(
            new CommandBuilder()
                .withName("addlaw")
                .withAliases("al", LOCALE_PREFIX + "CMDAddLaw")
                .withDescription("Add a law to your faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.addlaw")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "law",
                    new ArgumentBuilder()
                        .setDescription("the law to add")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                        
                )
        );
    }

    public void execute(CommandContext context) {
        context.getExecutorsFaction().addLaw(String.join(" ", context.getStringArgument("law")));
        context.replyWith(
            this.constructMessage("LawAdded")
                .with("law", context.getStringArgument("law"))
        );
    }
}