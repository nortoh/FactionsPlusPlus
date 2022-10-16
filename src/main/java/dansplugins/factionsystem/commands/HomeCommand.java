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
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.extended.Scheduler;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class HomeCommand extends SubCommand {
    private final Scheduler scheduler;
    private final LocaleService localeService;
    private final PlayerService playerService;
    private final PersistentData persistentData;

    @Inject
    public HomeCommand(
        PlayerService playerService,
        LocaleService localeService, 
        PersistentData persistentData,
        Scheduler scheduler
    ) {
        super();
        this.playerService = playerService;
        this.localeService = localeService;
        this.scheduler = scheduler;
        this.persistentData = persistentData;
        this
            .setNames("home", LOCALE_PREFIX + "CmdHome")
            .requiresPermissions("mf.home")
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
        if (this.faction.getFactionHome() == null) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("FactionHomeNotSetYet"),
                "FactionHomeNotSetYet", 
                false
            );
            return;
        }
        final Chunk home_chunk;
        if (!this.persistentData.getChunkDataAccessor().isClaimed(home_chunk = this.faction.getFactionHome().getChunk())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("HomeIsInUnclaimedChunk"),
                "HomeIsInUnclaimedChunk", 
                false
            );
            return;
        }
        ClaimedChunk chunk = this.persistentData.getChunkDataAccessor().getClaimedChunk(home_chunk);
        if (chunk == null || chunk.getHolder() == null) {
            this.playerService.sendMessage(
                player, "&c" + this.localeService.getText("HomeIsInUnclaimedChunk"),
                "HomeIsInUnclaimedChunk", 
                false
            );
            return;
        }
        if (!chunk.getHolder().equalsIgnoreCase(this.faction.getName())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("HomeClaimedByAnotherFaction"),
                "HomeClaimedByAnotherFaction", 
                false
            );
            return;
        }
        this.scheduler.scheduleTeleport(player, this.faction.getFactionHome());
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