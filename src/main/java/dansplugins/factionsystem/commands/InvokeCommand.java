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
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.misc.ArgumentParser;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class InvokeCommand extends SubCommand {

    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final MessageService messageService;
    private final PlayerService playerService;
    private final ConfigService configService;

    @Inject
    public InvokeCommand(
        ConfigService configService,
        PersistentData persistentData,
        LocaleService localeService,
        MessageService messageService,
        PlayerService playerService
    ) {
        super();
        this.configService = configService;
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.messageService = messageService;
        this.playerService = playerService;
        this
            .setNames("invoke", LOCALE_PREFIX + "CmdInvoke")
            .requiresPermissions("mf.invoke")
            .isPlayerCommand()
            .requiresPlayerInFaction()
            .requiresFactionOwner();
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
        if (args.length < 2) {
            player.sendMessage(
                this.translate("&c" + "Usage: /mf invoke \"ally\" \"enemy\"")
            );
            return;
        }
        ArgumentParser argumentParser = new ArgumentParser();
        final List<String> argumentsInsideDoubleQuotes = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (argumentsInsideDoubleQuotes.size() < 2) {
            player.sendMessage(ChatColor.RED + "Arguments must be designated in between double quotes.");
            return;
        }
        final Faction invokee = this.persistentData.getFaction(argumentsInsideDoubleQuotes.get(0));
        final Faction warringFaction = this.persistentData.getFaction(argumentsInsideDoubleQuotes.get(1));
        if (invokee == null || warringFaction == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", argumentsInsideDoubleQuotes.get(0)),
                true
            );
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", argumentsInsideDoubleQuotes.get(1)),
                true
            );
            return;
        }
        if (!this.faction.isAlly(invokee.getName()) && !this.faction.isVassal(invokee.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("NotAnAllyOrVassal", invokee.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("NotAnAllyOrVassal")).replace("#name#", invokee.getName()),
                true
            );
            return;
        }
        if (!this.faction.isEnemy(warringFaction.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("NotAtWarWith", warringFaction.getName()),
                messageService.getLanguage().getString("NotAtWarWith").replace("#name#", warringFaction.getName()),
                true
            );
            return;
        }
        if (this.configService.getBoolean("allowNeutrality") && ((boolean) invokee.getFlags().getFlag("neutral"))) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotBringNeutralFactionIntoWar"),
                "CannotBringNeutralFactionIntoWar",
                false
            );
            return;
        }
        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(invokee, warringFaction, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            invokee.addEnemy(warringFaction.getName());
            warringFaction.addEnemy(invokee.getName());

            this.messageFaction(
                invokee, // Message ally faction
                "&c" + this.localeService.getText("AlertCalledToWar1", this.faction.getName(), warringFaction.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertCalledToWar1"))
                    .replace("#f1#", this.faction.getName())
                    .replace("#f2#", warringFaction.getName())
            );

            this.messageFaction(
                warringFaction, // Message warring faction
                "&c" + this.localeService.getText("AlertCalledToWar2", this.faction.getName(), invokee.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertCalledToWar2"))
                    .replace("#f1#", faction.getName())
                    .replace("#f2#", invokee.getName())
            );

            this.messageFaction(
                this.faction, // Message player faction
                "&a" + this.localeService.getText("AlertCalledToWar3", invokee.getName(), warringFaction.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertCalledToWar3"))
                    .replace("#f1#", this.faction.getName())
                    .replace("#f2#", warringFaction.getName())
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
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            if (args.length == 1) {
                return TabCompleteTools.filterStartingWithAddQuotes(args[0], playerFaction.getAllies());
            } else if (args.length == 2) {
                return TabCompleteTools.filterStartingWithAddQuotes(args[0], playerFaction.getEnemyFactions());
            }
        }
        return null;
    }
}