/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class PrefixCommand extends Command {

    private final PersistentData persistentData;

    @Inject
    public PrefixCommand(PersistentData persistentData) {
        super(
            new CommandBuilder()
                .withName("prefix")
                .withAliases(LOCALE_PREFIX + "CmdPrefix")
                .withDescription("Sets your factions chat prefix.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.prefix")
                .addArgument(
                    "new prefix",
                    new ArgumentBuilder()
                        .setDescription("the new prefix for your faction")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                        
                )
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        final String newPrefix = context.getStringArgument("new prefix");
        if (this.persistentData.isPrefixTaken(newPrefix)) {
            context.replyWith("PrefixTaken");
            return;
        }
        context.getExecutorsFaction().setPrefix(newPrefix);
        context.replyWith("PrefixSet");
    }
}