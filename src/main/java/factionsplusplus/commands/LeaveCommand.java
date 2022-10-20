/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.events.FactionLeaveEvent;
import factionsplusplus.models.Faction;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.utils.Logger;
import factionsplusplus.builders.CommandBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class LeaveCommand extends Command {
    private final EphemeralData ephemeralData;
    private final Logger logger;
    private final DisbandCommand disbandCommand;

    @Inject
    public LeaveCommand(
        EphemeralData ephemeralData,
        Logger logger,
        DisbandCommand disbandCommand
    ) {
        super(
            new CommandBuilder()
                .withName("leave")
                .withAliases(LOCALE_PREFIX + "CmdLeave")
                .withDescription("Leave your current faction.")
                .requiresPermissions("mf.leave")
                .expectsPlayerExecution()
                .expectsFactionMembership()
        );
        this.ephemeralData = ephemeralData;
        this.logger = logger;
        this.disbandCommand = disbandCommand;
    }

    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        Faction faction = context.getExecutorsFaction();
        final boolean isOwner = faction.isOwner(player.getUniqueId());
        if (isOwner) {
            this.disbandCommand.execute(context); // Disband the Faction.
            return;
        }
        FactionLeaveEvent leaveEvent = new FactionLeaveEvent(faction, player);
        Bukkit.getPluginManager().callEvent(leaveEvent);
        if (leaveEvent.isCancelled()) {
            this.logger.debug("Leave event was cancelled.");
            return;
        }

        if (faction.isOfficer(player.getUniqueId())) faction.removeOfficer(player.getUniqueId()); // Remove Officer.
        this.ephemeralData.getPlayersInFactionChat().remove(player.getUniqueId()); // Remove from Faction Chat.
        faction.removeMember(player.getUniqueId());
        context.replyWith("AlertLeftFaction");
        context.messagePlayersFaction(
            this.constructMessage("AlertLeftFactionTeam")
                .with("name", player.getName())
                .with("faction", faction.getName())
        );
    }
}