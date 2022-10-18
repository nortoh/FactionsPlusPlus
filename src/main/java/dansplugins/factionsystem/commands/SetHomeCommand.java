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
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.builders.*;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class SetHomeCommand extends Command {

    private final PersistentData persistentData;
    private final PlayerService playerService;
    private final LocaleService localeService;

    @Inject
    public SetHomeCommand(PersistentData persistentData, PlayerService playerService, LocaleService localeService) {
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
        this.playerService = playerService;
        this.localeService = localeService;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        Faction faction = context.getExecutorsFaction();
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
        if (chunk == null || !chunk.getHolder().equalsIgnoreCase(faction.getName())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("CannotSetFactionHomeInWilderness"),
                "CannotSetFactionHomeInWilderness", 
                false
            );
            return;
        }
        faction.setFactionHome(player.getLocation());
        this.playerService.sendMessage(
            player, 
            "&a" + this.localeService.getText("FactionHomeSet"),
            "FactionHomeSet", 
            false
        );
    }
}