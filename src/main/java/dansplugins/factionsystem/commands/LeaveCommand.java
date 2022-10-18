/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.events.FactionLeaveEvent;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.builders.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class LeaveCommand extends Command {
    private final MessageService messageService;
    private final PlayerService playerService;
    private final LocaleService localeService;
    private final EphemeralData ephemeralData;
    private final Logger logger;
    private final DisbandCommand disbandCommand;

    @Inject
    public LeaveCommand(
        MessageService messageService,
        PlayerService playerService,
        LocaleService localeService,
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
        this.messageService = messageService;
        this.playerService = playerService;
        this.localeService = localeService;
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
        this.messageService.messageFaction(
            faction, 
            this.translate("&a" + player.getName() + " has left " + faction.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertLeftFactionTeam"))
                .replace("#name#", player.getName())
                .replace("#faction#", faction.getName())
        );
    }
}