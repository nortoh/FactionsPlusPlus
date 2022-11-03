/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.bukkit.Bukkit;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.builders.CommandBuilder;

@Singleton
public class AutoClaimCommand extends Command {

    @Inject
    public AutoClaimCommand() {
        super(
            new CommandBuilder()
                .withName("autoclaim")
                .withAliases("ac", LOCALE_PREFIX + "CmdAutoClaim")
                .withDescription("Toggles auto claim for faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.autoclaim")
                .expectsFactionMembership()
                .expectsFactionOwnership()
        );
    }

    public void execute(CommandContext context) {
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                final String localizationKey = context.getExecutorsFaction().toggleAutoClaim() ? "Enabled" : "Disabled";
                context.success("CommandResponse.Autoclaim."+localizationKey);
            }
        });
    }
}