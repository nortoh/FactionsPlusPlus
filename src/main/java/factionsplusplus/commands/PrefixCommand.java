/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.bukkit.Bukkit;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.services.DataService;
import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class PrefixCommand extends Command {

    private final DataService dataService;

    @Inject
    public PrefixCommand(DataService dataService) {
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
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        final String newPrefix = context.getStringArgument("new prefix");
        if (this.dataService.isFactionPrefixTaken(newPrefix)) {
            context.error("Error.Prefix.Taken", newPrefix);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), task -> {
            context.getExecutorsFaction().setPrefix(newPrefix);
            context.success("CommandResponse.Faction.PrefixSet", newPrefix);
        });
    }
}