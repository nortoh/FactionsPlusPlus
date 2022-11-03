/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import org.bukkit.Bukkit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

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
        final String description = context.getStringArgument("description");
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                context.getExecutorsFaction().setDescription(description);
                context.success("CommandResponse.Faction.DescriptionSet", description);
            }
        });
    }
}