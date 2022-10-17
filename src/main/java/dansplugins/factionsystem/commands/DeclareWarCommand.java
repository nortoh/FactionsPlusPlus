/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.factories.WarFactory;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.FactionService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.misc.ArgumentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DeclareWarCommand extends SubCommand {

    private final ConfigService configService;
    private final PlayerService playerService;
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
        PlayerService playerService,
        MessageService messageService,
        PersistentData persistentData,
        WarFactory warFactory,
        FactionRepository factionRepository,
        FactionService factionService
    ) {
        super();
        this.localeService = localeService;
        this.configService = configService;
        this.playerService = playerService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.warFactory = warFactory;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
        this
            .setNames("declarewar", "dw", LOCALE_PREFIX + "CmdDeclareWar")
            .requiresPermissions("mf.declarewar")
            .isPlayerCommand()
            .requiresPlayerInFaction()
            .requiresFactionOfficer();
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        if (args.length == 0) {
            this.playerService.sendMessage(
                player,
                "&c" + "Usage: /mf declarewar \"faction\"",
                "UsageDeclareWar",
                false
            );
            return;
        }

        ArgumentParser argumentParser = new ArgumentParser();
        List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);

        if (doubleQuoteArgs.size() == 0) {
            this.playerService.sendMessage(
                player,
                "&c" + "Usage: /mf declarewar \"faction\" (quotation marks are required)",
                "UsageDeclareWar",
                false
            );
            return;
        }

        String factionName = doubleQuoteArgs.get(0);

        final Faction opponent = this.factionRepository.get(factionName);
        if (opponent == null) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                true
            );
            return;
        }

        if (opponent == this.faction) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("CannotDeclareWarOnYourself"),
                "CannotDeclareWarOnYourself",
                false
            );
            return;
        }

        if (this.faction.isEnemy(opponent.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("AlertAlreadyAtWarWith"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAlreadyAtWarWith")).replace("#faction#", opponent.getName()),
                true
            );
            return;
        }

        if (this.faction.hasLiege() && opponent.hasLiege()) {
            if (this.faction.isVassal(opponent.getName())) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("CannotDeclareWarOnVassal"),
                    "CannotDeclareWarOnVassal",
                    false
                );
                return;
            }

            if (!this.faction.getLiege().equalsIgnoreCase(opponent.getLiege())) {
                final Faction enemyLiege = this.factionRepository.get(opponent.getLiege());
                if (this.factionService.calculateCumulativePowerLevelWithoutVassalContribution(enemyLiege) <
                        this.factionService.getMaximumCumulativePowerLevel(enemyLiege) / 2) {
                    this.playerService.sendMessage(
                        player,
                        "&c" + this.localeService.getText("CannotDeclareWarIfLiegeNotWeakened"),
                        "CannotDeclareWarIfLiegeNotWeakened",
                        false
                    );
                }
            }
        }

        if (this.faction.isLiege(opponent.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotDeclareWarOnLiege"),
                "CannotDeclareWarOnLiege",
                false
            );
            return;
        }

        if (this.faction.isAlly(opponent.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotDeclareWarOnAlly"),
                "CannotDeclareWarOnAlly",
                false
            );
            return;
        }

        if (this.configService.getBoolean("allowNeutrality") && (opponent.getFlag("neutral").toBoolean())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotDeclareWarOnNeutralFaction"),
                "CannotDeclareWarOnNeutralFaction",
                false
            );
            return;
        }

        if (this.configService.getBoolean("allowNeutrality") && (faction.getFlag("neutral").toBoolean())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotDeclareWarIfNeutralFaction"),
                "CannotDeclareWarIfNeutralFaction",
                false
            );
            return;
        }

        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(this.faction, opponent, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            // Make enemies.
            this.faction.addEnemy(opponent.getName());
            opponent.addEnemy(faction.getName());
            warFactory.createWar(this.faction, opponent);
            this.messageService.messageServer(
                "&c" + this.localeService.getText("HasDeclaredWarAgainst", this.faction.getName(), opponent.getName()), 
                Objects.requireNonNull(this.messageService.getLanguage().getString("HasDeclaredWarAgainst"))
                    .replace("#f_a#", this.faction.getName())
                    .replace("#f_b#", opponent.getName())
            );

        }
    }

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(CommandSender sender, String[] args, String key) {

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