/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import org.bukkit.Bukkit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.builders.CommandBuilder;

@Singleton
public class BypassCommand extends Command {

    @Inject
    public BypassCommand() {
        super(
            new CommandBuilder()
                .withName("bypass")
                .withAliases(LOCALE_PREFIX + "CmdBypass")
                .withDescription("Toggle bypass protections.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.bypass", "mf.admin")
        );
    }

    public void execute(CommandContext context) {
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), task -> {
            final boolean currentlyBypassing = context.getFPPPlayer().isAdminBypassing();
            context.getFPPPlayer().toggleAdminBypassing();
            context.success("PlayerNotice.AdminBypass." + (currentlyBypassing ? "Disabled" : "Enabled")); 
        });
    }
}