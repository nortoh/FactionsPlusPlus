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
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.DynmapIntegrationService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class UnclaimallCommand extends SubCommand {

    private PersistentData persistentData;
    private LocaleService localeService;
    private PlayerService playerService;
    private MessageService messageService;
    private DynmapIntegrationService dynmapService;
    private FactionRepository factionRepository;

    @Inject
    public UnclaimallCommand(
        PersistentData persistentData,
        LocaleService localeService,
        PlayerService playerService,
        MessageService messageService,
        DynmapIntegrationService dynmapService,
        FactionRepository factionRepository
    ) {
        super();
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.playerService = playerService;
        this.messageService = messageService;
        this.dynmapService = dynmapService;
        this.factionRepository = factionRepository;
        this
            .setNames("unclaimall", "ua", LOCALE_PREFIX + "CmdUnclaimall");
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
        final Faction faction;
        if (args.length == 0) {
            // Self
            if (!(sender instanceof Player)) {
                this.playerService.sendMessage(
                    sender, 
                    this.localeService.getText("OnlyPlayersCanUseCommand"),
                    "OnlyPlayersCanUseCommand", 
                    false
                );
                return;
            }
            List<String> missingPermissions = this.checkPermissions(sender, "mf.unclaimall");
            if (missingPermissions.size() > 0) {
                this.messageService.sendPermissionMissingMessage(sender, missingPermissions);
                return;
            }
            faction = this.playerService.getPlayerFaction(sender);
            if (faction == null) {
                this.playerService.sendMessage(
                    sender, 
                    "&c" + this.localeService.getText("AlertMustBeInFactionToUseCommand"),
                    "AlertMustBeInFactionToUseCommand", 
                    false
                );
                return;
            }
            if (!faction.isOwner(((Player) sender).getUniqueId())) {
                this.playerService.sendMessage(
                    sender, 
                    "&c" + this.localeService.getText("AlertMustBeOwnerToUseCommand"),
                    "AlertMustBeOwnerToUseCommand", 
                    false
                );
                return;
            }
        } else {
            List<String> missingPermissions = this.checkPermissions(sender, "mf.unclaimall.others", "mf.admin");
            if (missingPermissions.size() > 0) {
                this.messageService.sendPermissionMissingMessage(sender, missingPermissions);
                return;
            }
            faction = this.factionRepository.get(String.join(" ", args));
            if (faction == null) {
                this.playerService.sendMessage(
                    sender, 
                    "&c" + this.localeService.getText("FactionNotFound"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)), 
                    true
                );
                return;
            }
        }
        // remove faction home
        faction.setFactionHome(null);
        this.messageService.messageFaction(
            faction, 
            this.translate("&c" + this.localeService.getText("AlertFactionHomeRemoved")),
            this.messageService.getLanguage().getString("AlertFactionHomeRemoved")
        );

        // remove claimed chunks
        this.persistentData.getChunkDataAccessor().removeAllClaimedChunks(faction.getID());
        this.dynmapService.updateClaimsIfAble();
        this.playerService.sendMessage(
            sender, 
            "&a" + this.localeService.getText("AllLandUnclaimedFrom", faction.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AllLandUnclaimedFrom")).replace("#name#", faction.getName()), 
            false
        );

        // remove locks associated with this faction
        this.persistentData.removeAllLocks(faction.getID());
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}