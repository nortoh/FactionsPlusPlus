/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
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
public class TransferCommand extends SubCommand {

    private final PlayerService playerService;
    private final LocaleService localeService;
    private final MessageService messageService;
    private final PersistentData persistentData;

    @Inject
    public TransferCommand(
        PlayerService playerService,
        LocaleService localeService,
        MessageService messageService,
        PersistentData persistentData
    ) {
        super();
        this.playerService = playerService;
        this.localeService = localeService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this
            .setNames("transfer", LOCALE_PREFIX + "CmdTransfer")
            .requiresPermissions("mf.transfer")
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
                "&c" + this.localeService.getText("UsageTransfer"),
                "UsageTransfer",
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
                "&c" + this.localeService.getText("PlayerIsNotInYourFaction"),
                "PlayerIsNotInYourFaction",
                false
            );
            return;
        }
        if (targetUUID.equals(player.getUniqueId())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotTransferToSelf"),
                "CannotTransferToSelf",
                false
            );
            return;
        }

        if (this.faction.isOfficer(targetUUID)) this.faction.removeOfficer(targetUUID); // Remove Officer (if there is one)

        // set owner
        this.faction.setOwner(targetUUID);
        this.playerService.sendMessage(
            player,
            "&b" + this.localeService.getText("OwnerShipTransferredTo", args[0]),
            Objects.requireNonNull(this.messageService.getLanguage().getString("OwnerShipTransferredTo")).replace("#name#", args[0]),
            true
        );
        if (target.isOnline() && target.getPlayer() != null) { // Message if we can :)
            this.playerService.sendMessage(
                target.getPlayer(),
                "&a" + this.localeService.getText("OwnershipTransferred", this.faction.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("'OwnershipTransferred")).replace("#name#", this.faction.getName()),
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