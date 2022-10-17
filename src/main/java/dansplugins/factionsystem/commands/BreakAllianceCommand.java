/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class BreakAllianceCommand extends SubCommand {

    private final PlayerService playerService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final FactionRepository factionRepository;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public BreakAllianceCommand(PlayerService playerService, MessageService messageService, PersistentData persistentData, LocaleService localeService, FactionRepository factionRepository) {
        super();
        this.playerService = playerService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.factionRepository = factionRepository;
        this
            .setNames("breakalliance", "ba", LOCALE_PREFIX + "CmdBreakAlliance")
            .requiresPermissions("mf.breakalliance")
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
        if (args.length == 0) {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("UsageBreakAlliance"), "UsageBreakAlliance", false);
            return;
        }

        final Faction otherFaction = this.factionRepository.get(String.join(" ", args));
        if (otherFaction == null) {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("FactionNotFound"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound"))
                            .replace("#faction#", String.join(" ", args)), true);
            return;
        }

        if (otherFaction == this.faction) {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("CannotBreakAllianceWithSelf"), "CannotBreakAllianceWithSelf", false);
            return;
        }

        if (!this.faction.isAlly(otherFaction.getName())) {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("AlertNotAllied", otherFaction.getName()),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNotAllied"))
                            .replace("#faction#", otherFaction.getName()), true);
            return;
        }

        this.faction.removeAlly(otherFaction.getName());
        otherFaction.removeAlly(this.faction.getName());
        this.messageFaction(
            this.faction, 
            this.translate("&c" + this.localeService.getText("AllianceBrokenWith", otherFaction.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AllianceBrokenWith"))
                .replace("#faction#", otherFaction.getName())
        );
        this.messageFaction(
            otherFaction, 
            this.translate("&c" + this.localeService.getText("AlertAllianceHasBeenBroken", this.faction.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAllianceHasBeenBroken"))
                .replace("#faction#", this.faction.getName())
        );
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
        final List<String> factionsAllowedtoAlly = new ArrayList<>();
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            ArrayList<String> playerAllies = playerFaction.getAllies();
            return TabCompleteTools.filterStartingWith(args[0], playerAllies);
        }
        return null;
    }
}