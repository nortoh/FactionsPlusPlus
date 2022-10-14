/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class CheckAccessCommand extends SubCommand {

    private final PlayerService playerService;
    private final EphemeralData ephemeralData;

    @Inject
    public CheckAccessCommand(PlayerService playerService, EphemeralData ephemeralData) {
        super();
        this.playerService = playerService;
        this.ephemeralData = ephemeralData;
        this
            .setNames("checkaccess", "ca", LOCALE_PREFIX + "CmdCheckAccess")
            .requiresPermissions("mf.checkaccess")
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
        boolean cancel = false, contains = this.ephemeralData.getPlayersCheckingAccess().contains(player.getUniqueId());

        if (args.length >= 1) {
            cancel = args[0].equalsIgnoreCase("cancel");
        }

        if (cancel && contains) {
            this.ephemeralData.getPlayersCheckingAccess().remove(player.getUniqueId());
            this.playerService.sendMessage(player, "&c" + this.getText("Cancelled"), "Cancelled", false);
        } else {
            if (contains) {
                this.playerService.sendMessage(player, "&c" + this.getText("AlreadyEnteredCheckAccess"), "AlreadyEnteredCheckAccess", false);
            } else {
                this.ephemeralData.getPlayersCheckingAccess().add(player.getUniqueId());
                this.playerService.sendMessage(player, "&a" + this.getText("RightClickCheckAccess"), "RightClickCheckAccess", false);
            }
        }
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
        return TabCompleteTools.completeSingleOption(args[0], "cancel");
    }
}