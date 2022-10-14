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
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class GrantAccessCommand extends SubCommand {

    private final LocaleService localeService;
    private final MessageService messageService;
    private final PlayerService playerService;

    @Inject
    public GrantAccessCommand(LocaleService localeService, MessageService messageService, PlayerService playerService) {
        super();
        this.localeService = localeService;
        this.messageService = messageService;
        this.playerService = playerService;
        this
            .setNames("grantaccess", "ga", LOCALE_PREFIX + "CmdGrantAccess")
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
        if (args.length == 0) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("UsageGrantAccess"),
                "UsageGrantAccess",
                false
            );
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CommandCancelled"),
                "CommandCancelled",
                false
            );
            return;
        }
        if (this.ephemeralData.getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("AlertAlreadyGrantingAccess"),
                "AlertAlreadyGrantingAccess",
                false
            );
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("PlayerNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]),
                true
            );
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotGrantAccessToSelf"),
                "CannotGrantAccessToSelf",
                false
            );
            return;
        }
        this.ephemeralData.getPlayersGrantingAccess().put(player.getUniqueId(), targetUUID);
        this.playerService.sendMessage(
            player,
            "&a" + this.localeService.getText("RightClickGrantAccess", args[0]),
            Objects.requireNonNull(this.messageService.getLanguage().getString("RightClickGrantAccess")).replace("#name#", args[0]),
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

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        return TabCompleteTools.allOnlinePlayersMatching(args[0]);
    }
}