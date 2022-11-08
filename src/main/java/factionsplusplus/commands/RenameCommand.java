/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.utils.Logger;
import org.bukkit.Bukkit;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.events.internal.FactionRenameEvent;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class RenameCommand extends Command {
    private final ConfigService configService;
    private final Logger logger;
    private final DataService dataService;

    @Inject
    public RenameCommand(
        ConfigService configService,
        Logger logger,
        DataService dataService
    ) {
        super(
            new CommandBuilder()
                .withName("rename")
                .withAliases(LOCALE_PREFIX + "CmdRename")
                .withDescription("Renames your faction.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.rename")
                .addArgument(
                    "new faction name",
                    new ArgumentBuilder()
                        .setDescription("the new name for your faction")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                        
                )
        );
        this.configService = configService;
        this.logger = logger;
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        final String newName = context.getStringArgument("new faction name");
        if (newName.length() > this.configService.getInt("faction.limits.name.length")) {
            context.error("Error.Faction.NameTooLong", newName);
            return;
        }
        final String oldName = context.getExecutorsFaction().getName();
        if (this.dataService.getFaction(newName) != null) {
            context.error("Error.Faction.AlreadyExists", newName);
            return;
        }
        final FactionRenameEvent renameEvent = new FactionRenameEvent(context.getExecutorsFaction(), oldName, newName);
        Bukkit.getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled()) {
            this.logger.debug("Rename event was cancelled.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), task -> {
            context.getExecutorsFaction().setName(newName); // setName will handle changing prefix too, if necessary
            context.success("CommandResponse.Faction.Renamed", newName);
        });
    }
}