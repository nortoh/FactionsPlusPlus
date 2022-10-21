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
import factionsplusplus.utils.extended.Messenger;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class WhoCommand extends Command {
    private final Messenger messenger;
    private final DataService dataService;

    @Inject
    public WhoCommand(
        Messenger messenger,
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
        this.messenger = messenger;
    }

    public void execute(CommandContext context) {
        final Faction temp = this.dataService.getPlayersFaction(context.getOfflinePlayerArgument("player"));
        if (temp == null) {
            context.replyWith("PlayerIsNotInAFaction");
            return;
        }
        this.messenger.sendFactionInfo(
            context.getPlayer(), 
            temp,
            this.dataService.getClaimedChunkRepository().getAllForFaction(temp).size()
        );
    }
}