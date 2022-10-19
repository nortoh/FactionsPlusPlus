/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;
import dansplugins.factionsystem.builders.MessageBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Callum Johnson
 */
@Singleton
public class InviteCommand extends Command {
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final MedievalFactions medievalFactions;

    @Inject
    public InviteCommand(
        MessageService messageService,
        PersistentData persistentData,
        MedievalFactions medievalFactions
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
                        .isRequired()
                )
        );
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.medievalFactions = medievalFactions;
    }

    public void execute(CommandContext context) {
        Faction faction = context.getExecutorsFaction();
        Player player = context.getPlayer();
        if (faction.getFlag("mustBeOfficerToInviteOthers").toBoolean()) {
            // officer or owner rank required
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId())) {
                context.replyWith("AlertMustBeOwnerOrOfficerToUseCommand");
                return;
            }
        }
        final OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID playerUUID = target.getUniqueId();
        if (this.persistentData.isInFaction(playerUUID)) {
            context.replyWith("PlayerAlreadyInFaction");
            return;
        }
        faction.invite(playerUUID);
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
        getServer().getScheduler().runTaskLater(this.medievalFactions, () -> {
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

    /*
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        return TabCompleteTools.allOnlinePlayersMatching(args[0]);
    }
}