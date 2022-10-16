/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.PlayerRecord;
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
public class PowerCommand extends SubCommand {

    private final MessageService messageService;
    private final PersistentData persistentData;
    private final PlayerService playerService;
    private final LocaleService localeService;

    @Inject
    public PowerCommand(
        MessageService messageService,
        PersistentData persistentData,
        PlayerService playerService,
        LocaleService localeService
    ) {
        super();
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.playerService = playerService;
        this.localeService = localeService;
        this
            .setNames("power", LOCALE_PREFIX + "CmdPower")
            .requiresPermissions("mf.power");
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
        final PlayerRecord record;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                this.playerService.sendMessage(
                    sender,
                    this.localeService.getText("OnlyPlayersCanUseCommand"),
                    "OnlyPlayersCanUseCommand",
                    false
                );
                return;
            }
            record = this.persistentData.getPlayerRecord(((Player) sender).getUniqueId());
            this.playerService.sendMessage(
                sender,
                "&b" + this.localeService.getText("AlertCurrentPowerLevel", record.getPower(), record.maxPower()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertCurrentPowerLevel"))
                    .replace("#power#", String.valueOf(record.getPower()))
                    .replace("#max#", String.valueOf(record.maxPower())),
                true
            );
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID target = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (target == null) {
            this.playerService.sendMessage(
                sender,
                "&c" + this.localeService.getText("PlayerNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]),
                true
            );
            return;
        }
        record = this.persistentData.getPlayerRecord(target);
        this.playerService.sendMessage(
            sender, 
            "&b" + this.localeService.getText("CurrentPowerLevel", args[0], record.getPower(), record.maxPower()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("CurrentPowerLevel"))
                .replace("#power#", String.valueOf(record.getPower()))
                .replace("#max#", String.valueOf(record.maxPower()))
                .replace("#name#", args[0]),
            true
        );
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return TabCompleteTools.allOnlinePlayersMatching(args[0]);
    }
}