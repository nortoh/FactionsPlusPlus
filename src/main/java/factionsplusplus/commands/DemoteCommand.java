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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class DemoteCommand extends Command {

    @Inject
    public DemoteCommand() {
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
    }

    public void execute(CommandContext context) {
        OfflinePlayer playerToBeDemoted = context.getOfflinePlayerArgument("player");

        if (playerToBeDemoted.getUniqueId().equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotDemoteSelf");
            return;
        }

        context.getExecutorsFaction().removeOfficer(playerToBeDemoted.getUniqueId());

        if (playerToBeDemoted.isOnline()) {
            context.messagePlayer(playerToBeDemoted.getPlayer(), "AlertDemotion");
        }
        context.replyWith(
            this.constructMessage("PlayerDemoted")
                .with("name", playerToBeDemoted.getName())
        );
    }
}