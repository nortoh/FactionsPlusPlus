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
public class LockCommand extends SubCommand {

    private final PlayerService playerService;
    private final EphemeralData ephemeralData;
    private final LocaleService localeService;

    @Inject
    public LockCommand(PlayerService playerService, EphemeralData ephemeralData, LocaleService localeService) {
        super();
        this.playerService = playerService;
        this.ephemeralData = ephemeralData;
        this.localeService = localeService;
        this
            .setNames("lock", LOCALE_PREFIX + "CmdLock")
            .requiresPermissions("mf.lock")
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
        if (args.length >= 1 && this.safeEquals(args[0], "cancel")) {
            if (this.ephemeralData.getLockingPlayers().remove(player.getUniqueId())) { // Remove them
                this.playerService.sendMessage(
                    player, 
                    "&c" + this.localeService.getText("LockingCancelled"),
                    "LockingCancelled", 
                    false
                );
                return;
            }
        }
        this.ephemeralData.getLockingPlayers().add(player.getUniqueId());
        this.ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
        this.playerService.sendMessage(
            player, 
            "&a" + this.localeService.getText("RightClickLock"),
            "RightClickLock", 
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