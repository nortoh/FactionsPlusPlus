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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import factionsplusplus.builders.CommandBuilder;

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
        context.getExecutorsAudience().sendMessage(
            Component.text("Factions Plus Plus v"+this.pluginVerson).color(NamedTextColor.AQUA)
        );
    }
}