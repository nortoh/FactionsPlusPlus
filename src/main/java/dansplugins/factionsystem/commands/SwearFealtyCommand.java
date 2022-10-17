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
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class SwearFealtyCommand extends SubCommand {

    private final LocaleService localeService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final PlayerService playerService;
    private final FactionRepository factionRepository;

    @Inject
    public SwearFealtyCommand(
        LocaleService localeService,
        MessageService messageService,
        PersistentData persistentData,
        PlayerService playerService,
        FactionRepository factionRepository
    ) {
        super();
        this.localeService = localeService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.playerService = playerService;
        this.factionRepository = factionRepository;
        this
            .setNames("swearfealty", "sf", LOCALE_PREFIX + "CmdSwearFealty")
            .requiresPermissions("mf.swearfealty")
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
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("UsageSwearFealty"),
                "UsageSwearFealty",
                false
            );
            return;
        }
        final Faction target = this.factionRepository.get(String.join(" ", args));
        if (target == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                true
            );
            return;
        }
        if (!target.hasBeenOfferedVassalization(faction.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("AlertNotOfferedVassalizationBy"),
                "AlertNotOfferedVassalizationBy",
                false
            );
            return;
        }
        // set vassal
        target.addVassal(faction.getName());
        target.removeAttemptedVassalization(faction.getName());

        // set liege
        faction.setLiege(target.getName());

        // inform target faction that they have a new vassal
        this.messageService.messageFaction(
            target,
            this.translate("&a" + this.localeService.getText("AlertFactionHasNewVassal", faction.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertFactionHasNewVassal"))
                .replace("#name#", faction.getName())
        );

        // inform players faction that they have a new liege
        this.messageService.messageFaction(
            faction,
            this.translate("&a" + this.localeService.getText("AlertFactionHasBeenVassalized", target.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertFactionHasBeenVassalized"))
                .replace("#name#", target.getName())
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
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}