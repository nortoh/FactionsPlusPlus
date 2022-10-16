/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class ChatCommand extends SubCommand {

    private final PlayerService playerService;
    private final LocaleService localeService;
    private final EphemeralData ephemeralData;

    @Inject
    public ChatCommand(PlayerService playerService, EphemeralData ephemeralData, LocaleService localeService) {
        super();
        this.playerService = playerService;
        this.ephemeralData = ephemeralData;
        this.localeService = localeService;
        this
            .setNames("chat", LOCALE_PREFIX + "CmdChat")
            .requiresPermissions("mf.chat")
            .isPlayerCommand()
            .requiresPlayerInFaction();
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
        final boolean contains = this.ephemeralData.getPlayersInFactionChat().contains(player.getUniqueId());

        final String path = (contains ? "NoLonger" : "NowSpeaking") + "InFactionChat";

        if (contains) {
            this.ephemeralData.getPlayersInFactionChat().remove(player.getUniqueId());
        } else {
            this.ephemeralData.getPlayersInFactionChat().add(player.getUniqueId());
        }
        this.playerService.sendMessage(player, "&c" + this.localeService.getText(path), path, false);
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