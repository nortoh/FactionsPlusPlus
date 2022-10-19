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
import factionsplusplus.builders.*;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class SetHomeCommand extends Command {

    private final PersistentData persistentData;

    @Inject
    public SetHomeCommand(PersistentData persistentData) {
        super(
            new CommandBuilder()
                .withName("sethome")
                .withAliases("sh", LOCALE_PREFIX + "CmdSetHome")
                .withDescription("Set your faction home.")
                .expectsPlayerExecution()
                .expectsFactionOfficership()
                .requiresPermissions("mf.sethome")
        );
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        Faction faction = context.getExecutorsFaction();
        if (!this.persistentData.getChunkDataAccessor().isClaimed(player.getLocation().getChunk())) {
            context.replyWith("LandIsNotClaimed");
            return;
        }
        ClaimedChunk chunk = this.persistentData.getChunkDataAccessor().getClaimedChunk(player.getLocation().getChunk());
        if (chunk == null || !chunk.getHolder().equals(faction.getID())) {
            context.replyWith("CannotSetFactionHomeInWilderness");
            return;
        }
        faction.setFactionHome(player.getLocation());
        context.replyWith("FactionHomeSet");
    }
}