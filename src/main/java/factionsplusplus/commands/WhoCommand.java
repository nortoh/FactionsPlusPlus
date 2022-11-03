/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.DataService;
import factionsplusplus.services.FactionService;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class WhoCommand extends Command {
    private final FactionService factionService;
    private final DataService dataService;

    @Inject
    public WhoCommand(
        FactionService factionService,
        DataService dataService
    ) {
        super(
            new CommandBuilder()
                .withName("who")
                .withAliases(LOCALE_PREFIX + "CmdWho")
                .withDescription("Look up a players faction.")
                .requiresPermissions("mf.who")
                .expectsPlayerExecution()
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the player to look up their joined faction")
                        .expectsAnyPlayer()
                        .isRequired()
                )
        );
        this.dataService = dataService;
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        final Faction temp = this.dataService.getPlayersFaction(context.getOfflinePlayerArgument("player"));
        if (temp == null) {
            context.error("Error.Player.NotMemberOfFaction", context.getOfflinePlayerArgument("player").getName());
            return;
        }
        context.replyWith(this.factionService.generateFactionInfo(temp));
    }
}