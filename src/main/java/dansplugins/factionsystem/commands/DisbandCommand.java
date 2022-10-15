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
import dansplugins.factionsystem.events.FactionDisbandEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class DisbandCommand extends SubCommand {

    private final PlayerService playerService;
    private final MessageService messageService;
    private final ConfigService configService;
    private final PersistentData persistentData;
    private final EphemeralData ephemeralData;
    private final DynmapIntegrator dynmapIntegrator;
    private final Logger logger;
    private final LocaleService localeService;

    @Inject
    public DisbandCommand(
        PlayerService playerService,
        MessageService messageService,
        ConfigService configService,
        Logger logger,
        LocaleService localeService,
        PersistentData persistentData,
        DynmapIntegrator dynmapIntegrator,
        EphemeralData ephemeralData
    ) {
        super();
        this.localeService = localeService;
        this.playerService = playerService;
        this.messageService = messageService;
        this.configService = configService;
        this.logger = logger;
        this.persistentData = persistentData;
        this.dynmapIntegrator = dynmapIntegrator;
        this.ephemeralData = ephemeralData;
        this
            .setNames("disband", LOCALE_PREFIX + "CmdDisband");
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
        final Faction disband;
        final boolean self;
        if (args.length == 0) {
            if (!this.checkPermissions(sender, "mf.disband")) return;
            if (!(sender instanceof Player)) { // ONLY Players can be in a Faction
                if (!this.configService.getBoolean("useNewLanguageFile")) {
                    sender.sendMessage(this.translate(this.localeService.getText("OnlyPlayersCanUseCommand")));
                } else {
                    this.playerService.sendMessageToConsole(sender.getServer().getConsoleSender(), "OnlyPlayersCanUseCommand", true);
                }
                return;
            }
            disband = this.playerService.getPlayerFaction(sender);
            self = true;
            if (disband.getPopulation() != 1) {
                this.playerService.sendMessage(
                    sender,
                    "&c" + this.localeService.getText("AlertMustKickAllPlayers"),
                    "AlertMustKickAllPlayers",
                    false
                );
                return;
            }
        } else {
            if (!this.checkPermissions(sender, "mf.disband.others", "mf.admin")) return;
            disband = this.persistentData.getFaction(String.join(" ", args));
            self = false;
        }
        if (disband == null) {
            this.playerService.sendMessage(
                sender,
                "&c" + this.localeService.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                true
            );
            return;
        }
        final int factionIndex = this.persistentData.getFactionIndexOf(disband);
        if (self) {
            this.playerService.sendMessage(
                sender,
                "&c" + this.localeService.getText("FactionSuccessfullyDisbanded"),
                "FactionSuccessfullyDisbanded", 
                false
            );
            this.ephemeralData.getPlayersInFactionChat().remove(((Player) sender).getUniqueId());
        } else {
            this.playerService.sendMessage(
                sender,
                "&c" + this.localeService.getText("SuccessfulDisbandment", disband.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("SuccessfulDisbandment")).replace("#faction#", disband.getName()), 
                true
            );
        }
        this.removeFaction(factionIndex, self ? ((OfflinePlayer) sender) : null);
    }

    private void removeFaction(int i, OfflinePlayer disbandingPlayer) {

        Faction disbandingThisFaction = this.persistentData.getFactionByIndex(i);
        String nameOfFactionToRemove = disbandingThisFaction.getName();
        FactionDisbandEvent event = new FactionDisbandEvent(
                disbandingThisFaction,
                disbandingPlayer
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.logger.debug("Disband event was cancelled.");
            return;
        }

        // remove claimed land objects associated with this faction
        this.persistentData.getChunkDataAccessor().removeAllClaimedChunks(nameOfFactionToRemove);
        this.dynmapIntegrator.updateClaims();

        // remove locks associated with this faction
        this.persistentData.removeAllLocks(this.persistentData.getFactionByIndex(i).getName());

        this.persistentData.removePoliticalTiesToFaction(nameOfFactionToRemove);

        this.persistentData.removeFactionByIndex(i);
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        if (! this.checkPermissions(sender)) return null;
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}