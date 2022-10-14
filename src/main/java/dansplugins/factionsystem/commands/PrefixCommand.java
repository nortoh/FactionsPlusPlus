/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class PrefixCommand extends SubCommand {

    private final PersistentData persistentData;
    private final PlayerService playerService;
    private final LocaleService localeService;

    @Inject
    public PrefixCommand(PersistentData persistentData, PlayerService playerService, LocaleService localeService) {
        super();
        this.persistentData = persistentData;
        this.playerService = playerService;
        this.localeService = localeService;
        this
            .setNames("prefix", LOCALE_PREFIX + "CmdPrefix")
            .requiresPermissions("mf.prefix")
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
        final String newPrefix = String.join(" ", args);
        if (this.persistentData.isPrefixTaken(newPrefix)) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("PrefixTaken"),
                "PrefixTaken",
                false
            );
            return;
        }
        this.faction.setPrefix(newPrefix);
        this.playerService.sendMessage(
            player,
            "&c" + this.localeService.getText("PrefixSet"),
            "PrefixSet",
            false
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