/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * @author Callum Johnson
 */
@Singleton
public class LawsCommand extends SubCommand {

    private final LocaleService localeService;
    private final PlayerService playerService;
    private final MessageService messageService;
    private final PersistentData persistentData;

    @Inject
    public LawsCommand(
        LocaleService localeService,
        PlayerService playerService,
        MessageService messageService,
        PersistentData persistentData
    ) {
        super();
        this.localeService = localeService;
        this.playerService = playerService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this
            .setNames("laws", LOCALE_PREFIX + "CmdLaws")
            .requiresPermissions("mf.laws")
            .isPlayerCommand();
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
        final Faction target;
        if (args.length == 0) {
            target = this.playerService.getPlayerFaction(player);
            if (target == null) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("AlertMustBeInFactionToUseCommand"),
                    "AlertMustBeInFactionToUseCommand",
                    false
                );
                return;
            }
            if (target.getNumLaws() == 0) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("AlertNoLaws"),
                    "AlertNoLaws",
                    false
                );
                return;
            }
        } else {
            target = this.persistentData.getFaction(String.join(" ", args));
            if (target == null) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("FactionNotFound"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                    true
                );
                return;
            }
            if (target.getNumLaws() == 0) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("FactionDoesNotHaveLaws"),
                    "FactionDoesNotHaveLaws",
                    false
                );
                return;
            }
        }
        this.playerService.sendMessage(
            player,
            "&b" + this.localeService.getText("LawsTitle", target.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("LawsTitle")).replace("#name#", target.getName()),
            true
        );
        IntStream.range(0, target.getNumLaws())
                .mapToObj(i -> translate("&b" + (i + 1) + ". " + target.getLaws().get(i)))
                .forEach(player::sendMessage);
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