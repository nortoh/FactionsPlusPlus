/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.FactionService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
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
public class PromoteCommand extends SubCommand {

    private final MessageService messageService;
    private final LocaleService localeService;
    private final PlayerService playerService;
    private final PersistentData persistentData;
    private final FactionService factionService;

    @Inject
    public PromoteCommand(
        MessageService messageService,
        LocaleService localeService,
        PlayerService playerService,
        PersistentData persistentData,
        FactionService factionService
    ) {
        super();
        this.messageService = messageService;
        this.localeService = localeService;
        this.playerService = playerService;
        this.persistentData = persistentData;
        this.factionService = factionService;
        this
            .setNames("promote", LOCALE_PREFIX + "CmdPromote")
            .requiresPermissions("mf.promote")
            .isPlayerCommand()
            .requiresPlayerInFaction()
            .requiresFactionOwner();
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
                "&c" + this.localeService.getText("UsagePromote"),
                "UsagePromote",
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
        if (!this.faction.isMember(targetUUID)) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("PlayerIsNotMemberOfFaction"),
                "PlayerIsNotMemberOfFaction",
                false
            );
            return;
        }
        if (this.faction.isOfficer(targetUUID)) {
            playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("PlayerAlreadyOfficer"),
                "PlayerAlreadyOfficer",
                false
            );
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotPromoteSelf"),
                "CannotPromoteSelf",
                false
            );
            return;
        }
        int maxOfficers = this.factionService.calculateMaxOfficers(this.faction);
        if (this.faction.getOfficerList().size() <= maxOfficers) {
            this.faction.addOfficer(targetUUID);
            this.playerService.sendMessage(
                player,
                "&a" + this.localeService.getText("PlayerPromoted"),
                "PlayerPromoted",
                false
            );
            if (target.isOnline() && target.getPlayer() != null) {
                this.playerService.sendMessage(
                    target.getPlayer(),
                    "&a" + this.localeService.getText("PromotedToOfficer"),
                    "PromotedToOfficer",
                    false
                );
            }
        } else {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("PlayerCantBePromotedBecauseOfLimit", maxOfficers),
                Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerCantBePromotedBecauseOfLimit")).replace("#number#", String.valueOf(this.faction.calculateMaxOfficers())), 
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
                    if (!playerFaction.getOfficerList().contains(uuid)) {
                        membersInFaction.add(member.getName());
                    }
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], membersInFaction);
        }
        return null;
    }
}