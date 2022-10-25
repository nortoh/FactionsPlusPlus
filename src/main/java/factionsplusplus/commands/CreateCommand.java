/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.events.FactionCreateEvent;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.FactionService;
import org.bukkit.Bukkit;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class CreateCommand extends Command {
    private final ConfigService configService;
    private final FactionRepository factionRepository;
    private final FactionService factionService;

    @Inject
    public CreateCommand(
        ConfigService configService,
        FactionRepository factionRepository,
        FactionService factionService
    ) {
        super(
            new CommandBuilder()
                .withName("create")
                .withAliases(LOCALE_PREFIX + "CmdCreate")
                .withDescription("creates a new faction")
                .expectsPlayerExecution()
                .requiresPermissions("mf.create")
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("a name for your new faction")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                        
                )
        );
        this.configService = configService;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        if (context.getExecutorsFaction() != null) {
            context.replyWith("AlreadyInFaction");
            return;
        }
        final String factionName = context.getStringArgument("faction name");
        if (factionName.length() > this.configService.getInt("factionMaxNameLength")) {
            context.replyWith(
                this.constructMessage("FactionNameTooLong")
                    .with("name", factionName)
            );
            return;
        }

        if (this.factionRepository.get(factionName) != null) {
            context.replyWith(
                this.constructMessage("FactionAlreadyExists")
                    .with("name", factionName)
            );
            return;
        }

        Faction playerFaction = this.factionService.createFaction(factionName, context.getPlayer().getUniqueId());
        playerFaction.setOwner(context.getPlayer().getUniqueId());
        FactionCreateEvent createEvent = new FactionCreateEvent(playerFaction, context.getPlayer());
        Bukkit.getPluginManager().callEvent(createEvent);
        if (! createEvent.isCancelled()) {
            this.factionRepository.create(playerFaction);
            context.replyWith(
                this.constructMessage("FactionCreated")
                    .with("name", factionName)
            );
        }
    }
}