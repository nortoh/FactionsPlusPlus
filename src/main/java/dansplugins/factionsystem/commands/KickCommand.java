/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionKickEvent;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class KickCommand extends Command {
    private final MessageService messageService;
    private final PlayerService playerService;
    private final LocaleService localeService;
    private final PersistentData persistentData;
    private final EphemeralData ephemeralData;
    private final Logger logger;

    @Inject
    public KickCommand(
        MessageService messageService,
        PlayerService playerService,
        LocaleService localeService,
        EphemeralData ephemeralData,
        PersistentData persistentData,
        Logger logger
    ) {
        super(
            new CommandBuilder()
                .withName("kick")
                .withAliases(LOCALE_PREFIX + "CmdKick")
                .withDescription("Kicks another player from your faction.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOfficership()
                .requiresPermissions("mf.kick")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the player to kick from your faction")
                        .expectsFactionMember()
                        .isRequired()
                )
        );
        this.messageService = messageService;
        this.playerService = playerService;
        this.localeService = localeService;
        this.ephemeralData = ephemeralData;
        this.persistentData = persistentData;
        this.logger = logger;
    }

    public void execute(CommandContext context) {
        OfflinePlayer target = context.getOfflinePlayerArgument("player");
        if (target.getUniqueId().equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotKickSelf");
            return;
        }
        Player player = context.getPlayer();
        Faction faction = context.getExecutorsFaction();
        if (faction.isOwner(target.getUniqueId())) {
            context.replyWith("CannotKickOwner");
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(faction, target, player);
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            this.logger.debug("Kick event was cancelled.");
            return;
        }
        if (faction.isOfficer(target.getUniqueId())) {
            faction.removeOfficer(target.getUniqueId()); // Remove Officer (if one)
        }
        this.ephemeralData.getPlayersInFactionChat().remove(target.getUniqueId());
        faction.removeMember(target.getUniqueId());
        this.messageService.messageFaction(
            faction,
            "&c" + this.localeService.getText("HasBeenKickedFrom", target.getName(), faction.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasBeenKickedFrom"))
                    .replace("#name#", target.getName())
                    .replace("#faction#", faction.getName())
        );
        if (target.isOnline() && target.getPlayer() != null) {
            this.playerService.sendMessage(
                target.getPlayer(),
                "&c" + this.localeService.getText("AlertKicked", player.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertKicked")).replace("#name#", player.getName()),
                true
            );
        }
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        final List<String> membersInFaction = new ArrayList<>();
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            for (UUID uuid : playerFaction.getMemberList()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    membersInFaction.add(member.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], membersInFaction);
        }
        return null;
    }
}