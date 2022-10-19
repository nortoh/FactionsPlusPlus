/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.factories.WarFactory;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.FactionService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DeclareWarCommand extends Command {

    private final ConfigService configService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final WarFactory warFactory;
    private final FactionRepository factionRepository;
    private final FactionService factionService;

    @Inject
    public DeclareWarCommand(
        ConfigService configService,
        LocaleService localeService,
        MessageService messageService,
        PersistentData persistentData,
        WarFactory warFactory,
        FactionRepository factionRepository,
        FactionService factionService
    ) {
        super(
            new CommandBuilder()
                .withName("grantindependence")
                .withAliases("dw", LOCALE_PREFIX + "CmdDeclareWar")
                .withDescription("Declare war on a faction.")
                .requiresPermissions("mf.declarewar")
                .expectsPlayerExecution()
                .expectsNoFactionMembership()
                .expectsFactionOfficership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to declare war on")
                        .expectsFaction()
                        .expectsDoubleQuotes()
                        .isRequired()
                )
                .addArgument(
                    "reason",
                    new ArgumentBuilder()
                        .setDescription("the reason for declaring war")
                        .expectsString()
                        .expectsDoubleQuotes()
                        .isOptional()
                )
        );
        this.localeService = localeService;
        this.configService = configService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.warFactory = warFactory;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        final Player player = context.getPlayer();
        final Faction faction = context.getExecutorsFaction();
        final Faction opponent = context.getFactionArgument("faction name");

        if (opponent == faction) {
            context.replyWith("CannotDeclareWarOnYourself");
            return;
        }

        if (faction.isEnemy(opponent.getName())) {
            context.replyWith(
                this.constructMessage("AlertAlreadyAtWarWith")
                    .with("faction", opponent.getName())
            );
            return;
        }

        if (faction.hasLiege() && opponent.hasLiege()) {
            if (faction.isVassal(opponent.getName())) {
                context.replyWith("CannotDeclareWarOnVassal");
                return;
            }

            if (!faction.getLiege().equalsIgnoreCase(opponent.getLiege())) {
                final Faction enemyLiege = this.factionRepository.get(opponent.getLiege());
                if (this.factionService.calculateCumulativePowerLevelWithoutVassalContribution(enemyLiege) <
                        this.factionService.getMaximumCumulativePowerLevel(enemyLiege) / 2) {
                    context.replyWith("CannotDeclareWarIfLiegeNotWeakened");
                }
            }
        }

        if (faction.isLiege(opponent.getName())) {
            context.replyWith("CannotDeclareWarOnLiege");
            return;
        }

        if (faction.isAlly(opponent.getName())) {
            context.replyWith("CannotDeclareWarOnAlly");
            return;
        }

        if (this.configService.getBoolean("allowNeutrality") && (opponent.getFlag("neutral").toBoolean())) {
            context.replyWith("CannotDeclareWarOnNeutralFaction");
            return;
        }

        if (this.configService.getBoolean("allowNeutrality") && (faction.getFlag("neutral").toBoolean())) {
            context.replyWith("CannotDeclareWarIfNeutralFaction");
            return;
        }

        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(faction, opponent, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            // Make enemies.
            faction.addEnemy(opponent.getName());
            opponent.addEnemy(faction.getName());
            warFactory.createWar(faction, opponent, context.getStringArgument("reason"));
            this.messageService.messageServer(
                "&c" + this.localeService.getText("HasDeclaredWarAgainst", faction.getName(), opponent.getName()), 
                Objects.requireNonNull(this.messageService.getLanguage().getString("HasDeclaredWarAgainst"))
                    .replace("#f_a#", faction.getName())
                    .replace("#f_b#", opponent.getName())
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
            final List<String> factionsAllowedtoWar = new ArrayList<>();
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            ArrayList<String> playerEnemies = playerFaction.getEnemyFactions();
            ArrayList<String> playerAllies = playerFaction.getAllies();
            for(Faction faction : this.persistentData.getFactions()) {
                // If the faction is not an ally and they are not already enemied to them
                if(!playerAllies.contains(faction.getName()) && !playerEnemies.contains(faction.getName()) && !faction.getName().equalsIgnoreCase(playerFaction.getName())) {
                    factionsAllowedtoWar.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWithAddQuotes(args[0], factionsAllowedtoWar);
        }
        return null;
    }
}