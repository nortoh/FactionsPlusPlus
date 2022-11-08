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
import factionsplusplus.models.Faction;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.ArgumentFilterType;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class SwearFealtyCommand extends Command {

    @Inject
    public SwearFealtyCommand() {
        super(
            new CommandBuilder()
                .withName("swearfelty")
                .withAliases("sf", LOCALE_PREFIX + "CmdSwearFealty")
                .withDescription("Swear fealty to a faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.swearfealty")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to swear fealty to")
                        .expectsFaction()
                        .addFilters(ArgumentFilterType.OfferedVassalization)
                        .consumesAllLaterArguments()
                        .isRequired() 
                )
        );
    }

    public void execute(CommandContext context) {
        final Faction faction = context.getExecutorsFaction();
        final Faction target = context.getFactionArgument("faction name");
        if (! target.hasBeenOfferedVassalization(faction.getUUID())) {
            context.error("Error.Vassalization.NotOffered", target.getName());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), new Runnable() {
            @Override
            public void run() {
                // set vassal
                target.upsertRelation(faction.getUUID(), FactionRelationType.Vassal);
                target.removeAttemptedVassalization(faction.getUUID());
                
                // inform target faction that they have a new vassal
                target.alert("FactionNotice.NewVassal", faction.getName());
                // inform players faction that they have a new liege
                context.getExecutorsFaction().alert("FactionNotice.Vassalized", target.getName());
            }
        });
    }
}