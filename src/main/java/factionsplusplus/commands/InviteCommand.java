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
import factionsplusplus.services.MessageService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import factionsplusplus.builders.CommandBuilder;
import factionsplusplus.builders.ArgumentBuilder;
import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.constants.ArgumentFilterType;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Callum Johnson
 */
@Singleton
public class InviteCommand extends Command {
    private final MessageService messageService;
    private final DataService dataService;
    private final FactionsPlusPlus factionsPlusPlus;

    @Inject
    public InviteCommand(
        MessageService messageService,
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
        this.messageService = messageService;
        this.dataService = dataService;
        this.factionsPlusPlus = factionsPlusPlus;
    }

    public void execute(CommandContext context) {
        Faction faction = context.getExecutorsFaction();
        Player player = context.getPlayer();
        if (faction.getFlag("mustBeOfficerToInviteOthers").toBoolean()) {
            // officer or owner rank required
            if (! faction.isOfficer(player.getUniqueId()) && ! faction.isOwner(player.getUniqueId())) {
                context.replyWith("AlertMustBeOwnerOrOfficerToUseCommand");
                return;
            }
        }
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID playerUUID = target.getUniqueId();
        if (this.dataService.isPlayerInFaction(target)) {
            context.replyWith("PlayerAlreadyInFaction");
            return;
        }
        this.dataService.addFactionInvite(faction, player);
        context.replyWith("InvitationSent");
        if (target.isOnline() && target.getPlayer() != null) {
            context.messagePlayer(
                target.getPlayer(),
                this.constructMessage("AlertBeenInvited")
                    .with("name", faction.getName())
            );
        }

        final long seconds = 1728000L;
        // make invitation expire in 24 hours, if server restarts it also expires since invites aren't saved
        final OfflinePlayer tmp = target;
        getServer().getScheduler().runTaskLater(this.factionsPlusPlus, () -> {
            faction.uninvite(playerUUID);
            if (tmp.isOnline() && tmp.getPlayer() != null) {
                messageService.sendLocalizedMessage(
                    tmp.getPlayer(),
                    new MessageBuilder("InvitationExpired")
                        .with("name", faction.getName())
                );
            }
        }, seconds);
    }
}