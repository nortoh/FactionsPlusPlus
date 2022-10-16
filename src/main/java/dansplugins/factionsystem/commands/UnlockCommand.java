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
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Callum Johnson
 */
@Singleton
public class UnlockCommand extends SubCommand {

    private final EphemeralData ephemeralData;
    private final PlayerService playerService;
    private final LocaleService localeService;

    @Inject
    public UnlockCommand(
        EphemeralData ephemeralData,
        PlayerService playerService,
        LocaleService localeService
    ) {
        super();
        this.ephemeralData = ephemeralData;
        this.playerService = playerService;
        this.localeService = localeService;
        this
            .setNames("unlock", LOCALE_PREFIX + "CmdUnlock")
            .requiresPermissions("mf.unlock")
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
        if (args.length != 0 && args[0].equalsIgnoreCase("cancel")) {
            this.ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
            this.ephemeralData.getForcefullyUnlockingPlayers().remove(player.getUniqueId()); // just in case the player tries to cancel a forceful unlock without using the force command
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("AlertUnlockingCancelled"),
                "AlertUnlockingCancelled", 
                false
            );
            return;
        }
        if (!this.ephemeralData.getUnlockingPlayers().contains(player.getUniqueId())) {
            this.ephemeralData.getUnlockingPlayers().add(player.getUniqueId());
        }
        this.ephemeralData.getLockingPlayers().remove(player.getUniqueId());

        // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
        this.playerService.sendMessage(
            player, 
            "&a" + this.localeService.getText("RightClickUnlock"),
            "RightClickUnlock", 
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