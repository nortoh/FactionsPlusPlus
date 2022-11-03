/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.builders.CommandBuilder;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class ChatCommand extends Command {

    private final EphemeralData ephemeralData;

    @Inject
    public ChatCommand(EphemeralData ephemeralData) {
        super(
            new CommandBuilder()
                .withName("chat")
                .withAliases(LOCALE_PREFIX + "CmdChat")
                .withDescription("Toggle faction chat.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .requiresPermissions("mf.chat")
        );
        this.ephemeralData = ephemeralData;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        final boolean contains = this.ephemeralData.getPlayersInFactionChat().contains(player.getUniqueId());

        final String path = "PlayerNotice.FactionChat." + (contains ? "Left" : "Joined");

        if (contains) {
            this.ephemeralData.getPlayersInFactionChat().remove(player.getUniqueId());
        } else {
            this.ephemeralData.getPlayersInFactionChat().add(player.getUniqueId());
        }
        context.replyWith(path);
    }
}