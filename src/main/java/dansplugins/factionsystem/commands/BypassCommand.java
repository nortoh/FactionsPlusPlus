/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class BypassCommand extends SubCommand {

    private final EphemeralData ephemeralData;
    private final PlayerService playerService;

    /**
     * Constructor to initialise a Command.
     */
    @Inject
    public BypassCommand(PlayerService playerService, EphemeralData ephemeralData) {
        super();
        this.playerService = playerService;
        this.ephemeralData = ephemeralData;
        this
            .setNames("bypass", LOCALE_PREFIX + "CmdBypass")
            .requiresPermissions("mf.bypass", "mf.admin")
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
        final boolean contains = this.ephemeralData.getAdminsBypassingProtections().contains(player.getUniqueId());

        final String path = (contains ? "NoLonger" : "Now") + "BypassingProtections";

        if (contains) {
            this.ephemeralData.getAdminsBypassingProtections().remove(player.getUniqueId());
        } else {
            this.ephemeralData.getAdminsBypassingProtections().add(player.getUniqueId());
        }
        this.playerService.sendMessage(player, "&a" + this.getText(path), path, false);
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