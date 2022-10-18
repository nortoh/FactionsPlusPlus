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
import dansplugins.factionsystem.utils.Logger;
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
public class VassalizeCommand extends SubCommand {

    private final MessageService messageService;
    private final PlayerService playerService;
    private final Logger logger;
    private final LocaleService localeService;
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    
    @Inject
    public VassalizeCommand(
        MessageService messageService,
        PlayerService playerService,
        LocaleService localeService,
        PersistentData persistentData,
        Logger logger,
        FactionRepository factionRepository
    ) {
        super();
        this.messageService = messageService;
        this.playerService =playerService;
        this.localeService = localeService;
        this.persistentData = persistentData;
        this.logger = logger;
        this.factionRepository = factionRepository;
        this
            .setNames("vassalize", LOCALE_PREFIX + "CmdVassalize")
            .requiresPermissions("mf.vassalize")
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
                "&c" + this.localeService.getText("UsageVassalize"),
                "UsageVassalize",
                false
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
        // make sure player isn't trying to vassalize their own faction
        if (this.faction.getID().equals(target.getID())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotVassalizeSelf"),
                "CannotVassalizeSelf",
                false
            );
            return;
        }
        // make sure player isn't trying to vassalize their liege
        if (target.getID().equals(faction.getLiege())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotVassalizeLiege"),
                "CannotVassalizeLiege",
                false
            );
            return;
        }
        // make sure player isn't trying to vassalize a vassal
        if (target.hasLiege()) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("CannotVassalizeVassal"),
                "CannotVassalizeVassal",
                false
            );
            return;
        }
        // make sure this vassalization won't result in a vassalization loop
        final int loopCheck = this.willVassalizationResultInLoop(faction, target);
        if (loopCheck == 1 || loopCheck == 2) {
            this.logger.debug("Vassalization was cancelled due to potential loop");
            return;
        }
        // add faction to attemptedVassalizations
        this.faction.addAttemptedVassalization(target.getID());

        // inform all players in that faction that they are trying to be vassalized
        this.messageService.messageFaction(
            target, 
            this.translate("&a" + this.localeService.getText("AlertAttemptedVassalization", this.faction.getName(), this.faction.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAttemptedVassalization"))
                .replace("#name#", this.faction.getName())
        );

        // inform all players in players faction that a vassalization offer was sent
        this.messageService.messageFaction(
            this.faction,
            this.translate("&a" + this.localeService.getText("AlertFactionAttemptedToVassalize", target.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertFactionAttemptedToVassalize"))
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

    private int willVassalizationResultInLoop(Faction vassalizer, Faction potentialVassal) {
        final int MAX_STEPS = 1000;
        Faction current = vassalizer;
        int steps = 0;
        while (current != null && steps < MAX_STEPS) { // Prevents infinite loop and NPE (getFaction can return null).
            UUID liegeID = current.getLiege();
            if (liegeID == null) return 0;
            if (liegeID.equals(potentialVassal.getID())) return 1;
            current = this.persistentData.getFactionByID(liegeID);
            steps++;
        }
        return 2; // We don't know :/
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
            ArrayList<String> vassalizeableFactions = new ArrayList<>();
            for (Faction faction : this.persistentData.getFactions()) {
                if (!playerFaction.getVassals().contains(faction.getID())) {
                    vassalizeableFactions.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], vassalizeableFactions);
        }
        return null;
    }
    
}