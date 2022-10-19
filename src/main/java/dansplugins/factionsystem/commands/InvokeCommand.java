/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

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
    private final LocaleService localeService;
    private final MessageService messageService;
    private final ConfigService configService;

    @Inject
    public InvokeCommand(
        ConfigService configService,
        PersistentData persistentData,
        LocaleService localeService,
        MessageService messageService
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
                        .setDescription("the allied faction to invole")
                        .expectsAlliedFaction()
                        .expectsDoubleQuotes()
                        .isRequired()
                )
                .addArgument(
                    "enemy faction name",
                    new ArgumentBuilder()
                        .setDescription("the enemy faction to invole")
                        .expectsEnemyFaction()
                        .expectsDoubleQuotes()
                        .isRequired()
                )
        );
        this.configService = configService;
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.messageService = messageService;
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

            this.messageService.messageFaction(
                invokee, // Message ally faction
                "&c" + this.localeService.getText("AlertCalledToWar1", context.getExecutorsFaction().getName(), warringFaction.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertCalledToWar1"))
                    .replace("#f1#", context.getExecutorsFaction().getName())
                    .replace("#f2#", warringFaction.getName())
            );

            this.messageService.messageFaction(
                warringFaction, // Message warring faction
                "&c" + this.localeService.getText("AlertCalledToWar2", context.getExecutorsFaction().getName(), invokee.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertCalledToWar2"))
                    .replace("#f1#", context.getExecutorsFaction().getName())
                    .replace("#f2#", invokee.getName())
            );

            this.messageService.messageFaction(
                context.getExecutorsFaction(), // Message player faction
                "&a" + this.localeService.getText("AlertCalledToWar3", invokee.getName(), warringFaction.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertCalledToWar3"))
                    .replace("#f1#", context.getExecutorsFaction().getName())
                    .replace("#f2#", warringFaction.getName())
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