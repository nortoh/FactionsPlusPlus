/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.events.FactionRenameEvent;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.services.ConfigService;
import factionsplusplus.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class RenameCommand extends Command {
    private final PersistentData persistentData;
    private final ConfigService configService;
    private final Logger logger;

    @Inject
    public RenameCommand(
        PersistentData persistentData,
        ConfigService configService,
        Logger logger
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
        this.persistentData = persistentData;
        this.configService = configService;
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        final String newName = context.getStringArgument("new faction name");
        if (newName.length() > this.configService.getInt("factionMaxNameLength")) {
            context.replyWith(
                this.constructMessage("FactionNameTooLong")
                    .with("name", newName)
            );
            context.replyWith("FactionNameTooLong");
            return;
        }
        final String oldName = context.getExecutorsFaction().getName();
        if (this.persistentData.getFaction(newName) != null) {
            context.replyWith(
                this.constructMessage("FactionAlreadyExists")
                    .with("name", newName)
            );
            return;
        }
        final FactionRenameEvent renameEvent = new FactionRenameEvent(context.getExecutorsFaction(), oldName, newName);
        Bukkit.getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled()) {
            this.logger.debug("Rename event was cancelled.");
            return;
        }

        // change name
        context.getExecutorsFaction().setName(newName);
        context.replyWith("FactionNameChanged");

        // Prefix (if it was unset)
        if (context.getExecutorsFaction().getPrefix().equalsIgnoreCase(oldName)) context.getExecutorsFaction().setPrefix(newName);

        // Save again to overwrite current data
        this.persistentData.getLocalStorageService().save();
    }
}