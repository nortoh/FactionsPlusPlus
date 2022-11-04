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
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.DataService;
import factionsplusplus.builders.CommandBuilder;

@Singleton
public class BypassCommand extends Command {

    private final DataService dataService;

    @Inject
    public BypassCommand(DataService dataService) {
        super(
            new CommandBuilder()
                .withName("bypass")
                .withAliases(LOCALE_PREFIX + "CmdBypass")
                .withDescription("Toggle bypass protections.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.bypass", "mf.admin")
        );
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), task -> {
            PlayerRecord record = this.dataService.getPlayerRecord(context.getPlayer().getUniqueId());
            final boolean currentlyBypassing = record.isAdminBypassing();
            record.toggleAdminBypassing();
            context.success("PlayerNotice.AdminBypass." + (currentlyBypassing ? "Disabled" : "Enabled")); 
        });
    }
}