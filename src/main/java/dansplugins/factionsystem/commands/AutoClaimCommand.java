/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class AutoClaimCommand extends SubCommand {

    private final PlayerService playerService;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public AutoClaimCommand(PlayerService playerService) {
        super();
        this.playerService = playerService;
        this
            .setNames("autoclaim", "AC", LOCALE_PREFIX + "CmdAutoClaim")
            .requiresPermissions("mf.autoclaim")
            .isPlayerCommand()
            .requiresPlayerInFaction()
            .requiresFactionOwner();
    }

    /**
     * Method to execute the command.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        this.faction.toggleAutoClaim();
        this.playerService.sendMessage(player, "&b" + getText("AutoclaimToggled"), "AutoclaimToggled", false);
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