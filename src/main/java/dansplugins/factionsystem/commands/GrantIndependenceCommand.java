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
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class GrantIndependenceCommand extends SubCommand {

    private final PlayerService playerService;
    private final MessageService messageService;
    private final LocaleService localeService;
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    @Inject
    public GrantIndependenceCommand(
        PlayerService playerService,
        MessageService messageService,
        LocaleService localeService,
        PersistentData persistentData,
        FactionRepository factionRepository
    ) {
        super();
        this.playerService = playerService;
        this.messageService = messageService;
        this.localeService = localeService;
        this.persistentData = persistentData;
        this.factionRepository = factionRepository;
        this
            .setNames("grantindependence", "gi", LOCALE_PREFIX + "CmdGrantIndependence")
            .requiresPermissions("mf.grantindependence")
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
            player.sendMessage(
                this.translate("&c" + this.localeService.getText("UsageGrantIndependence"))
            );
            return;
        }
        final Faction target = this.factionRepository.get(String.join(" ", args));
        if (target == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                true
            );
            return;
        }
        if (!target.isLiege(this.faction.getID())) {
            player.sendMessage(this.translate("&c" + this.localeService.getText("FactionIsNotVassal")));
            return;
        }
        target.setLiege(null);
        this.faction.removeVassal(target.getID());
        // inform all players in that faction that they are now independent
        this.messageService.messageFaction(
            target,
            this.translate("&a" + this.localeService.getText("AlertGrantedIndependence", this.faction.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertGrantedIndependence"))
                .replace("#name#", faction.getName())
        );
        // inform all players in players faction that a vassal was granted independence
        this.messageService.messageFaction(
            this.faction,
            this.translate("&a" + this.localeService.getText("AlertNoLongerVassalFaction", target.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNoLongerVassalFaction"))
                .replace("#name#", target.getName())
        );
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
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            ArrayList<String> vassalNames = new ArrayList<>();
            for (UUID factionUUID : playerFaction.getVassals()) vassalNames.add(this.factionRepository.getByID(factionUUID).getName());
            return TabCompleteTools.filterStartingWith(args[0], vassalNames);
        }
        return null;
    }
}