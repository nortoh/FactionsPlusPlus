/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.services.DataService;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.builders.ArgumentBuilder;

/**
 * @author Callum Johnson
 */
@Singleton
public class DemoteCommand extends Command {

    private final DataService dataService;

    @Inject
    public DemoteCommand(DataService dataService) {
        super(
            new CommandBuilder()
                .withName("demote")
                .withAliases(LOCALE_PREFIX + "CmdDemote")
                .withDescription("Demote an officer of your faction.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.demote")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the officer to demote")
                        .expectsFactionOfficer()
                        .isRequired()
                )
        );
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        OfflinePlayer playerToBeDemoted = context.getOfflinePlayerArgument("player");

        if (playerToBeDemoted.getUniqueId().equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotDemoteSelf");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                context.getExecutorsFaction().upsertMember(playerToBeDemoted.getUniqueId(), GroupRole.Member);
                if (playerToBeDemoted.isOnline()) {
                    context.messagePlayer(playerToBeDemoted.getPlayer(), "AlertDemotion");
                }
                context.replyWith(
                    constructMessage("PlayerDemoted")
                        .with("name", playerToBeDemoted.getName())
                );
            }
        });
    }
}