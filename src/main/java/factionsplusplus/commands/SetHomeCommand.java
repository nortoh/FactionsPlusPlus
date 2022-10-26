/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.DataService;
import factionsplusplus.builders.*;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class SetHomeCommand extends Command {

    private final DataService dataService;

    @Inject
    public SetHomeCommand(DataService dataService) {
        super(
            new CommandBuilder()
                .withName("sethome")
                .withAliases("sh", LOCALE_PREFIX + "CmdSetHome")
                .withDescription("Set your faction home.")
                .expectsPlayerExecution()
                .expectsFactionOfficership()
                .requiresPermissions("mf.sethome")
        );
        this.dataService = dataService;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        Faction faction = context.getExecutorsFaction();
        if (! this.dataService.isChunkClaimed(player.getLocation().getChunk())) {
            context.replyWith("LandIsNotClaimed");
            return;
        }
        ClaimedChunk chunk = this.dataService.getClaimedChunk(player.getLocation().getChunk());
        if (chunk == null || ! chunk.getHolder().equals(faction.getID())) {
            context.replyWith("CannotSetFactionHomeInWilderness");
            return;
        }
        faction.setFactionHome(player.getLocation());
        context.replyWith("FactionHomeSet");
    }
}