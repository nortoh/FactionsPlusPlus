/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.DynmapIntegrationService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class UnclaimCommand extends SubCommand {

    private final EphemeralData ephemeralData;
    private final DynmapIntegrationService dynmapService;
    private final PersistentData persistentData;
    private final PlayerService playerService;
    private final MessageService messageService;

    @Inject
    public UnclaimCommand(
        EphemeralData ephemeralData,
        DynmapIntegrationService dynmapService,
        PersistentData persistentData,
        PlayerService playerService,
        MessageService messageService
    ) {
        super();
        this.ephemeralData = ephemeralData;
        this.persistentData = persistentData;
        this.dynmapService = dynmapService;
        this.playerService = playerService;
        this.messageService = messageService;
        this
            .setNames("unclaim", LOCALE_PREFIX + "CmdUnclaim")
            .requiresPermissions("mf.unclaim")
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
        final boolean isPlayerBypassing = this.ephemeralData.getAdminsBypassingProtections().contains(player.getUniqueId());
        if ((boolean) this.faction.getFlags().getFlag("mustBeOfficerToManageLand")) {
            // officer or owner rank required
            if (!this.faction.isOfficer(player.getUniqueId()) && !this.faction.isOwner(player.getUniqueId()) && !isPlayerBypassing) {
                this.playerService.sendMessage(
                    player, 
                    "&c" + "You're not able to claim land at this time.",
                    "NotAbleToClaim", 
                    false
                );
                return;
            }
        }
        if (args.length == 0) {
            this.persistentData.getChunkDataAccessor().removeChunkAtPlayerLocation(player, this.faction);
            this.dynmapService.updateClaimsIfAble();
            this.playerService.sendMessage(
                player, 
                "&a" + "Unclaimed your current claim.",
                "UnClaimed", 
                false
            );
            return;
        }
        int radius = this.getIntSafe(args[0], 1);
        if (radius <= 0) {
            radius = 1;
        }
        this.persistentData.getChunkDataAccessor().radiusUnclaimAtLocation(radius, player, this.faction);
        this.playerService.sendMessage(
            player, 
            "Unclaimed radius of " + radius + " claims around you!",
            Objects.requireNonNull(this.messageService.getLanguage().getString("UnClaimedRadius")).replace("#number#", String.valueOf(radius)), 
            true
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