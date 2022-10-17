/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.DynmapIntegrationService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class ClaimCommand extends SubCommand {

    private final PlayerService playerService;
    private final LocaleService localeService;
    private final PersistentData persistentData;
    private final DynmapIntegrationService dynmapService;

    @Inject
    public ClaimCommand(PlayerService playerService, LocaleService localeService, PersistentData persistentData, DynmapIntegrationService dynmapService) {
        super();
        this.localeService = localeService;
        this.playerService = playerService;
        this.persistentData = persistentData;
        this.dynmapService = dynmapService;
        this
            .setNames("claim", LOCALE_PREFIX + "CmdClaim")
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
        if ((boolean) this.faction.getFlags().getFlag("mustBeOfficerToManageLand")) {
            // officer or owner rank required
            if (!this.faction.isOfficer(player.getUniqueId()) && !this.faction.isOwner(player.getUniqueId())) {
                this.playerService.sendMessage(player, "&a" + this.localeService.getText("AlertMustBeOfficerOrOwnerToClaimLand"), "AlertMustBeOfficerOrOwnerToClaimLand", false);
                return;
            }
        }

        if (args.length != 0) {
            int depth = this.getIntSafe(args[0], -1);

            if (depth <= 0) {
                this.playerService.sendMessage(player, "&a" + this.localeService.getText("UsageClaimRadius"), "UsageClaimRadius", false);
            } else {
                this.persistentData.getChunkDataAccessor().radiusClaimAtLocation(depth, player, player.getLocation(), this.faction);
            }
        } else {
            this.persistentData.getChunkDataAccessor().claimChunkAtLocation(player, player.getLocation(), this.faction);
        }
        this.dynmapService.updateClaimsIfAble();
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