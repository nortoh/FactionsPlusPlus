/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.ClaimedChunk;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.extended.Scheduler;
import dansplugins.factionsystem.builders.*;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class HomeCommand extends Command {
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
        super(
            new CommandBuilder()
                .withName("home")
                .withAliases(LOCALE_PREFIX + "CmdHome")
                .withDescription("Teleport to your faction home.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .requiresPermissions("mf.home")
        );
        this.playerService = playerService;
        this.localeService = localeService;
        this.scheduler = scheduler;
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        Faction faction = context.getExecutorsFaction();
        if (faction.getFactionHome() == null) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("FactionHomeNotSetYet"),
                "FactionHomeNotSetYet", 
                false
            );
            return;
        }
        final Chunk home_chunk;
        if (!this.persistentData.getChunkDataAccessor().isClaimed(home_chunk = faction.getFactionHome().getChunk())) {
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
        if (!chunk.getHolder().equalsIgnoreCase(faction.getName())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("HomeClaimedByAnotherFaction"),
                "HomeClaimedByAnotherFaction", 
                false
            );
            return;
        }
        this.scheduler.scheduleTeleport(player, faction.getFactionHome());
    }
}