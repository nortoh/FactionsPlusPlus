/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.models.Command;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;
import factionsplusplus.services.DataService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;
import factionsplusplus.constants.ArgumentFilterType;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

@Singleton
public class InviteCommand extends Command {
    private final DataService dataService;
    private final FactionsPlusPlus factionsPlusPlus;

    @Inject
    public InviteCommand(
        DataService dataService,
        FactionsPlusPlus factionsPlusPlus
    ) {
        super(
            new CommandBuilder()
                .withName("invite")
                .withAliases(LOCALE_PREFIX + "CmdInvite")
                .withDescription("Invites a player to your facton.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the player to invite")
                        .expectsAnyPlayer()
                        .addFilters(ArgumentFilterType.NotInAnyFaction)
                        .isRequired()
                )
        );
        this.dataService = dataService;
        this.factionsPlusPlus = factionsPlusPlus;
    }

    public void execute(CommandContext context) {
        Faction faction = context.getExecutorsFaction();
        Player player = context.getPlayer();
        if (faction.getFlag("mustBeOfficerToInviteOthers").toBoolean()) {
            // officer or owner rank required
            if (! faction.isOfficer(player.getUniqueId()) && ! faction.isOwner(player.getUniqueId())) {
                context.error("Error.Faction.RoleOrAboveNeeded", context.getLocalizedString("Generic.Role.Officer"));
                return;
            }
        }
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID playerUUID = target.getUniqueId();
        if (this.dataService.isPlayerInFaction(target)) {
            context.error("Error.AlreadyInFaction.Other", target.getName());
            return;
        }
        this.dataService.addFactionInvite(faction, target);
        context.success("CommandResponse.Faction.InviteSent", target.getName(), faction.getName());
        if (target.isOnline() && target.getPlayer() != null) {
            context.alertPlayer(target, "PlayerNotice.FactionInvitation", faction.getName());
        }

        final long seconds = 1728000L;
        // TODO: move this to a stored procedure and scheduled task
        // make invitation expire in 24 hours, if server restarts it also expires since invites aren't saved
        final OfflinePlayer tmp = target;
        getServer().getScheduler().runTaskLater(this.factionsPlusPlus, () -> {
            faction.uninvite(playerUUID);
            if (tmp.isOnline() && tmp.getPlayer() != null) {
                context.alertPlayer(tmp.getPlayer(), "PlayerNotice.FactionInvitation.Expired", faction.getName());
            }
        }, seconds);
    }
}