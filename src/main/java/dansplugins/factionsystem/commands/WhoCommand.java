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
import dansplugins.factionsystem.utils.extended.Messenger;
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
public class WhoCommand extends SubCommand {
    private final PlayerService playerService;
    private final LocaleService localeService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final Messenger messenger;

    @Inject
    public WhoCommand(
        PlayerService playerService,
        LocaleService localeService,
        MessageService messageService,
        PersistentData persistentData,
        Messenger messenger
    ) {
        super();
        this.playerService = playerService;
        this.localeService = localeService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.messenger = messenger;
        this
            .setNames("who", LOCALE_PREFIX + "CmdWho")
            .requiresPermissions("mf.who")
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
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("UsageWho"), "UsageWho", false);
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
        final Faction temp = this.playerService.getPlayerFaction(targetUUID);
        if (temp == null) {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("PlayerIsNotInAFaction")
                    , "PlayerIsNotInAFaction", false);
            return;
        }
        this.messenger.sendFactionInfo(
            player, 
            temp,
            this.persistentData.getChunkDataAccessor().getChunksClaimedByFaction(temp.getName())
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