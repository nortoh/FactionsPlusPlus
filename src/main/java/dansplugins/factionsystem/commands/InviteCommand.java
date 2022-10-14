/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Callum Johnson
 */
@Singleton
public class InviteCommand extends SubCommand {
    private final PlayerService playerService;
    private final MessageService messageService;
    private final LocaleService localeService;
    private final PersistentData persistentData;
    private final MedievalFactions medievalFactions;

    @Inject
    public InviteCommand(
        PlayerService playerService,
        MessageService messageService,
        LocaleService localeService,
        PersistentData persistentData,
        MedievalFactions medievalFactions
    ) {
        super();
        this.playerService = playerService;
        this.messageService = messageService;
        this.localeService = localeService;
        this.persistentData = persistentData;
        this.medievalFactions = medievalFactions;
        this
            .setNames("invite", LOCALE_PREFIX + "CmdInvite")
            .requiresPermissions("mf.invite")
            .requiresPlayerInFaction()
            .isPlayerCommand();
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
            player.sendMessage(this.translate("&c" + this.localeService.getText("UsageInvite")));
            return;
        }
        if ((boolean) this.faction.getFlags().getFlag("mustBeOfficerToInviteOthers")) {
            // officer or owner rank required
            if (!this.faction.isOfficer(player.getUniqueId()) && !this.faction.isOwner(player.getUniqueId())) {
                this.playerService.sendMessage(
                    player, 
                    "&c" + this.localeService.getText("AlertMustBeOwnerOrOfficerToUseCommand"),
                    "AlertMustBeOwnerOrOfficerToUseCommand", 
                    false
                );
                return;
            }
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (playerUUID == null) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("PlayerNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]),
                true
            );
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerUUID);
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
        if (this.persistentData.isInFaction(playerUUID)) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("PlayerAlreadyInFaction"),
                "PlayerAlreadyInFaction", 
                false
            );
            return;
        }
        this.faction.invite(playerUUID);
        player.sendMessage(ChatColor.GREEN + this.localeService.get("InvitationSent"));
        if (target.isOnline() && target.getPlayer() != null) {
            this.playerService.sendMessage(
                    target.getPlayer(),
                    "&a" + this.localeService.getText("AlertBeenInvited", this.faction.getName(), this.faction.getName()),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("AlertBeenInvited")).replace("#name#", this.faction.getName()),
                    true
            );
        }

        final long seconds = 1728000L;
        // make invitation expire in 24 hours, if server restarts it also expires since invites aren't saved
        final OfflinePlayer tmp = target;
        getServer().getScheduler().runTaskLater(this.medievalFactions, () -> {
            this.faction.uninvite(playerUUID);
            if (tmp.isOnline() && tmp.getPlayer() != null) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.localeService.getText("InvitationExpired", this.faction.getName()),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("InvitationExpired")).replace("#name#", this.faction.getName()),
                    true
                );
            }
        }, seconds);
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
        return TabCompleteTools.allOnlinePlayersMatching(args[0]);
    }
}