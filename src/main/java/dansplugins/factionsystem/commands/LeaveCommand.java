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
import dansplugins.factionsystem.events.FactionLeaveEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class LeaveCommand extends SubCommand {
    private final Logger logger;
    private final DisbandCommand disbandCommand;

    @Inject
    public LeaveCommand(final Logger logger, final DisbandCommand disbandCommand) {
        super();
        this
            .setNames("leave", LOCALE_PREFIX + "CmdLeave")
            .requiresPermissions("mf.leave")
            .isPlayerCommand()
            .requiresPlayerInFaction();
        this.logger = logger;
        this.disbandCommand = disbandCommand;
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
        final boolean isOwner = this.faction.isOwner(player.getUniqueId());
        if (isOwner) {
            this.disbandCommand.execute((CommandSender) player, args, key); // Disband the Faction.
            return;
        }
        FactionLeaveEvent leaveEvent = new FactionLeaveEvent(this.faction, player);
        Bukkit.getPluginManager().callEvent(leaveEvent);
        if (leaveEvent.isCancelled()) {
            this.logger.debug("Leave event was cancelled.");
            return;
        }

        if (this.faction.isOfficer(player.getUniqueId())) this.faction.removeOfficer(player.getUniqueId()); // Remove Officer.
        this.ephemeralData.getPlayersInFactionChat().remove(player.getUniqueId()); // Remove from Faction Chat.
        this.faction.removeMember(player.getUniqueId());
        this.playerService.sendMessage(
            player, 
            "&b" + this.getText("AlertLeftFaction"),
            "AlertLeftFaction", 
            false
        );
        this.messageFaction(
            this.faction, 
            this.translate("&a" + player.getName() + " has left " + this.faction.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertLeftFactionTeam"))
                .replace("#name#", player.getName())
                .replace("#faction#", this.faction.getName())
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