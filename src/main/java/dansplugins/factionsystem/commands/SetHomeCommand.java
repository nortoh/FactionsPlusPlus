/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.ClaimedChunk;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class SetHomeCommand extends SubCommand {

    private final PersistentData persistentData;
    private final PlayerService playerService;
    private final LocaleService localeService;

    @Inject
    public SetHomeCommand(PersistentData persistentData, PlayerService playerService, LocaleService localeService) {
        super();
        this.persistentData = persistentData;
        this.playerService = playerService;
        this.localeService = localeService;
        this
            .setNames("sethome", "sh", LOCALE_PREFIX + "CmdSetHome")
            .requiresPermissions("mf.sethome")
            .isPlayerCommand()
            .requiresPlayerInFaction()
            .requiresFactionOfficer();
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
        if (!this.persistentData.getChunkDataAccessor().isClaimed(player.getLocation().getChunk())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("LandIsNotClaimed"),
                "LandIsNotClaimed", 
                false
            );
            return;
        }
        ClaimedChunk chunk = this.persistentData.getChunkDataAccessor().getClaimedChunk(player.getLocation().getChunk());
        if (chunk == null || !chunk.getHolder().equals(this.faction.getID())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("CannotSetFactionHomeInWilderness"),
                "CannotSetFactionHomeInWilderness", 
                false
            );
            return;
        }
        this.faction.setFactionHome(player.getLocation());
        this.playerService.sendMessage(
            player, 
            "&a" + this.localeService.getText("FactionHomeSet"),
            "FactionHomeSet", 
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