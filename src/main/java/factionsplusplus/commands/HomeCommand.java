/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.utils.extended.Scheduler;
import factionsplusplus.builders.CommandBuilder;
import org.bukkit.Chunk;

/**
 * @author Callum Johnson
 */
@Singleton
public class HomeCommand extends Command {
    private final Scheduler scheduler;
    private final PersistentData persistentData;

    @Inject
    public HomeCommand(
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
        this.scheduler = scheduler;
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        Faction faction = context.getExecutorsFaction();
        if (faction.getFactionHome() == null) {
            context.replyWith("FactionHomeNotSetYet");
            return;
        }
        final Chunk home_chunk;
        if (!this.persistentData.getChunkDataAccessor().isClaimed(home_chunk = faction.getFactionHome().getChunk())) {
            context.replyWith("HomeIsInUnclaimedChunk");
            return;
        }
        ClaimedChunk chunk = this.persistentData.getChunkDataAccessor().getClaimedChunk(home_chunk);
        if (chunk == null || chunk.getHolder() == null) {
            context.replyWith("HomeIsInUnclaimedChunk");
            return;
        }
        if (!chunk.getHolder().equals(faction.getID())) {
            context.replyWith("HomeClaimedByAnotherFaction");
            return;
        }
        this.scheduler.scheduleTeleport(context.getPlayer(), faction.getFactionHome());
    }
}