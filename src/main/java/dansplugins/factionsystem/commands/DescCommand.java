/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DescCommand extends SubCommand {

    private final PlayerService playerService;
    private final LocaleService localeService;
    private final MessageService messageService;

    @Inject
    public DescCommand(PlayerService playerService, LocaleService localeService, MessageService messageService) {
        super();
        this.localeService = localeService;
        this.playerService = playerService;
        this.messageService = messageService;
        this
            .setNames("description", "desc", LOCALE_PREFIX + "CmdDesc")
            .requiresPermissions("mf.desc")
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
                "&c" + this.localeService.getText("UsageDesc"),
                "UsageDesc", 
                false
            );
            return;
        }

        this.faction.setDescription(String.join(" ", args));
        this.playerService.sendMessage(
            player, 
            "&c" + this.localeService.getText("DescriptionSet"),
            Objects.requireNonNull(this.messageService.getLanguage().getString("Description")).replace("#desc#", String.join(" ", args)), 
            true
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
}