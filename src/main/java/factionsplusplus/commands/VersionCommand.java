/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.builders.*;

/**
 * @author Callum Johnson
 */
@Singleton
public class VersionCommand extends Command {
    private final String pluginVerson;

    @Inject
    public VersionCommand(@Named("pluginVersion") String pluginVersion) {
        super(
            new CommandBuilder()
                .withName("version")
                .withAliases(LOCALE_PREFIX + "CmdVersion")
                .withDescription("Check plugin version.")
                .requiresPermissions("mf.version")
        );
        this.pluginVerson = pluginVersion;
    }
    
    public void execute(CommandContext context) {
        context.getSender().sendMessage(this.translate("&bMedieval-Factions-" + this.pluginVerson));
    }
}