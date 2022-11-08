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
import factionsplusplus.services.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.events.internal.FactionWarStartEvent;
import factionsplusplus.builders.ArgumentBuilder;

@Singleton
public class InvokeCommand extends Command {

    private final ConfigService configService;

    @Inject
    public InvokeCommand(ConfigService configService) {
        super(
            new CommandBuilder()
                .withName("invoke")
                .withAliases(LOCALE_PREFIX + "CmdInvoke")
                .withDescription("Makes peace with an enemy faction.")
                .requiresPermissions("mf.invoke")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOfficership()
                .addArgument(
                    "allied faction name",
                    new ArgumentBuilder()
                        .setDescription("the allied faction to invoke")
                        .expectsAlliedFaction()
                        .expectsDoubleQuotes()
                        .isRequired()
                )
                .addArgument(
                    "enemy faction name",
                    new ArgumentBuilder()
                        .setDescription("the enemy faction to invoke")
                        .expectsEnemyFaction()
                        .expectsDoubleQuotes()
                        .isRequired()
                )
        );
        this.configService = configService;
    }

    public void execute(CommandContext context) {
        final Player player = context.getPlayer();
        final Faction invokee = context.getFactionArgument("allied faction name");
        final Faction warringFaction = context.getFactionArgument("enemy faction name");
        if (! context.getExecutorsFaction().isVassal(invokee.getUUID())) {
            context.error("Error.Faction.NotAllyOrVassal", invokee.getName());
            return;
        }
        if (this.configService.getBoolean("allowNeutrality") && (invokee.getFlag("neutral").toBoolean())) {
            context.error("Error.WarCall.Neutral");
            return;
        }
        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(invokee, warringFaction, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (! warStartEvent.isCancelled()) {
            Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), task -> {
                // Update relationship
                invokee.upsertRelation(warringFaction.getUUID(), FactionRelationType.Enemy);
                // Alert ally faction
                invokee.alert("FactionNotice.WarCall.Target", context.getExecutorsFaction().getName(), warringFaction.getName());
                // Alert warring faction
                warringFaction.alert("FactionNotice.WarCall.Enemy", context.getExecutorsFaction().getName(), invokee.getName());
                // Alert player faction
                context.getExecutorsFaction().alert("FactionNotice.WarCall.Source", invokee.getName(), warringFaction.getName());
            });
        }
    }
}