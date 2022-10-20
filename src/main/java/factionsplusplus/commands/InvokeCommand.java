/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.events.FactionWarStartEvent;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.repositories.FactionRepository;
import factionsplusplus.services.ConfigService;
import factionsplusplus.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
public class InvokeCommand extends Command {

    private final PersistentData persistentData;
    private final ConfigService configService;
    private final FactionRepository factionRepository;

    @Inject
    public InvokeCommand(
        ConfigService configService,
        PersistentData persistentData,
        FactionRepository factionRepository
    ) {
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
        this.persistentData = persistentData;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        final Player player = context.getPlayer();
        final Faction invokee = context.getFactionArgument("allied faction name");
        final Faction warringFaction = context.getFactionArgument("enemy faction name");
        if (!context.getExecutorsFaction().isVassal(invokee.getID())) {
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

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            if (args.length == 1) {
                ArrayList<String> allyFactionNames = new ArrayList<>();
                for (UUID uuid : playerFaction.getAllies()) allyFactionNames.add(this.factionRepository.getByID(uuid).getName());
                return TabCompleteTools.filterStartingWithAddQuotes(args[0], allyFactionNames);
            } else if (args.length == 2) {
                ArrayList<String> enemyFactionNames = new ArrayList<>();
                for (UUID uuid : playerFaction.getEnemyFactions()) enemyFactionNames.add(this.factionRepository.getByID(uuid).getName());
                return TabCompleteTools.filterStartingWithAddQuotes(args[0], enemyFactionNames);
            }
        }
        return null;
    }
}