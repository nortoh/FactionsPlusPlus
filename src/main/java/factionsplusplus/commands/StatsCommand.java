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

/**
 * @author Callum Johnson
 */
@Singleton
public class StatsCommand extends Command {

    private final PersistentData persistentData;

    @Inject
    public StatsCommand(
        PersistentData persistentData
    ) {
        super(
            new CommandBuilder()
                .withName("stats")
                .withAliases(LOCALE_PREFIX + "CmdStats")
                .withDescription("Retrieves plugin statistics.")
                .requiresPermissions("mf.stats")
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        context.getLocalizedStrings("StatsFaction")
            .forEach(s -> {
                if (s.contains("#faction#")) {
                    s = s.replace("#faction#", String.valueOf(this.persistentData.getNumFactions()));
                }
                if (s.contains("#players#")) {
                    s = s.replace("#players#", String.valueOf(this.persistentData.getNumPlayers()));
                }
                context.reply(s);
            });
    }
}