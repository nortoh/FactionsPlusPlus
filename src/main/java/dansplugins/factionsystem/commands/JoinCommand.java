/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionJoinEvent;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class JoinCommand extends SubCommand {
    private final PlayerService playerService;
    private final MessageService messageService;
    private final LocaleService localeService;
    private final Logger logger;
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    @Inject
    public JoinCommand(
        PlayerService playerService,
        MessageService messageService,
        LocaleService localeService,
        PersistentData persistentData,
        Logger logger,
        FactionRepository factionRepository
    ) {
        this.playerService = playerService;
        this.messageService = messageService;
        this.localeService = localeService;
        this.persistentData = persistentData;
        this.factionRepository = factionRepository;
        this.logger = logger;
        this
            .setNames("join", LOCALE_PREFIX + "CmdJoin")
            .requiresPermissions("mf.join")
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
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("UsageJoin"),
                "UsageJoin",
                false
            );
            return;
        }
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.localeService.getText("AlertAlreadyInFaction"),
                "AlertAlreadyInFaction",
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
        if (!target.isInvited(player.getUniqueId())) {
            this.playerService.sendMessage(
                player,
                "&c" + "You are not invited to this faction.",
                "NotInvite",
                false
            );
            return;
        }
        FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            this.logger.debug("Join event was cancelled.");
            return;
        }
        this.messageService.messageFaction(
            target,
            "&a" + this.localeService.getText("HasJoined", player.getName(), target.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasJoined"))
                .replace("#name#", player.getName())
                .replace("#faction#", target.getName())
        );
        target.addMember(player.getUniqueId());
        target.uninvite(player.getUniqueId());
        player.sendMessage(this.translate("&a" + this.localeService.getText("AlertJoinedFaction")));
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
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}