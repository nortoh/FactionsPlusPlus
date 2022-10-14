/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class CheckClaimCommand extends SubCommand {

    private final PersistentData.ChunkDataAccessor chunkDataAccessor;
    private final PlayerService playerService;

    @Inject
    public CheckClaimCommand(PlayerService playerService, PersistentData.ChunkDataAccessor chunkDataAccessor) {
        super();
        this.playerService = playerService;
        this.chunkDataAccessor = chunkDataAccessor;
        this
            .setNames("checkclaim", "cc", LOCALE_PREFIX + "CmdCheckClaim")
            .requiresPermissions("mf.checkclaim")
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
        final String result = this.chunkDataAccessor.checkOwnershipAtPlayerLocation(player);

        if (result.equals("unclaimed")) {
            this.playerService.sendMessage(player, "&a" + this.getText("LandIsUnclaimed"), "LandIsUnclaimed", false);
        } else {
            this.playerService.sendMessage(player, "&c" + this.getText("LandClaimedBy"), Objects.requireNonNull(this.messageService.getLanguage().getString("LandClaimedBy"))
                    .replace("#player#", result), true);
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
}