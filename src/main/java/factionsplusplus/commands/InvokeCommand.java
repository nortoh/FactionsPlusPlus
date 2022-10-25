/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.events.FactionWarStartEvent;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;


/**
 * @author Callum Johnson
 */
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
        if (! context.getExecutorsFaction().isVassal(invokee.getID())) {
            context.replyWith(
                this.constructMessage("NotAnAllyOrVassal")
                    .with("name", invokee.getName())
            );
            return;
        }
        if (this.configService.getBoolean("allowNeutrality") && (invokee.getFlag("neutral").toBoolean())) {
            context.replyWith("CannotBringNeutralFactionIntoWar");
            return;
        }
        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(invokee, warringFaction, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            invokee.addEnemy(warringFaction.getID());
            warringFaction.addEnemy(invokee.getID());

            // Alert ally faction
            context.messageFaction(
                invokee,
                this.constructMessage("AlertCalledToWar1")
                    .with("f1", context.getExecutorsFaction().getName())
                    .with("f2", warringFaction.getName())
            );

            // Alert warring faction
            context.messageFaction(
                warringFaction,
                this.constructMessage("AlertCalledToWar2")
                    .with("f1", context.getExecutorsFaction().getName())
                    .with("f2", invokee.getName())
            );

            // Alert player faction
            context.messagePlayersFaction(
                this.constructMessage("AlertCalledToWar3")
                    .with("f1", context.getExecutorsFaction().getName())
                    .with("f2", warringFaction.getName())
            );
        }
    }
}