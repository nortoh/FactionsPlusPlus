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
import dansplugins.factionsystem.events.FactionKickEvent;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class KickCommand extends SubCommand {
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
        super();
        this.messageService = messageService;
        this.playerService = playerService;
        this.localeService = localeService;
        this.ephemeralData = ephemeralData;
        this.persistentData = persistentData;
        this.logger = logger;
        this
            .setNames("kick", LOCALE_PREFIX + "CmdKick")
            .requiresPermissions("mf.kick")
            .isPlayerCommand()
            .requiresFactionOfficer()
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
        if (args.length == 0) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("UsageKick"),
                "UsageKick", 
                false
            );
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("PlayerNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]), 
                true
            );
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        if (!target.hasPlayedBefore()) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                this.playerService.sendMessage(
                    player, 
                    "&c" + this.localeService.getText("PlayerNotFound"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]), 
                    true
                );
                return;
            }
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotKickSelf"),
                "CannotKickSelf",
                false
            );
            return;
        }
        if (this.faction.isOwner(targetUUID)) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotKickOwner"),
                "CannotKickOwner",
                false
            );
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(this.faction, target, player);
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            this.logger.debug("Kick event was cancelled.");
            return;
        }
        if (this.faction.isOfficer(targetUUID)) {
            this.faction.removeOfficer(targetUUID); // Remove Officer (if one)
        }
        this.ephemeralData.getPlayersInFactionChat().remove(targetUUID);
        this.faction.removeMember(targetUUID);
        this.messageService.messageFaction(
            this.faction,
            "&c" + this.localeService.getText("HasBeenKickedFrom", target.getName(), this.faction.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasBeenKickedFrom"))
                    .replace("#name#", args[0])
                    .replace("#faction#", this.faction.getName())
        );
        if (target.isOnline() && target.getPlayer() != null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("AlertKicked", player.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertKicked")).replace("#name#", player.getName()),
                true
            );
        }
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