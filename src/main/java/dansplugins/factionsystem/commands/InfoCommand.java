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
import dansplugins.factionsystem.utils.extended.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class InfoCommand extends SubCommand {
    private final PlayerService playerService;
    private final LocaleService localeService;
    private final MessageService messageService;
    private final Messenger messenger;
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    @Inject
    public InfoCommand(
        PlayerService playerService,
        LocaleService localeService,
        MessageService messageService,
        Messenger messenger,
        PersistentData persistentData,
        FactionRepository factionRepository
    ) {
        super();
        this.playerService = playerService;
        this.localeService = localeService;
        this.messageService = messageService;
        this.messenger = messenger;
        this.persistentData = persistentData;
        this.factionRepository = factionRepository;
        this
            .setNames("info", LOCALE_PREFIX + "CmdInfo");
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
        final Faction target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                this.playerService.sendMessage(
                    sender,
                    this.localeService.getText("OnlyPlayersCanUseCommand"),
                    "OnlyPlayersCanUseCommand",
                    false
                );
                return;
            }
            target = this.playerService.getPlayerFaction(sender);
            if (target == null) {
                this.playerService.sendMessage(
                    sender,
                    "&c" + this.localeService.getText("AlertMustBeInFactionToUseCommand"),
                    "AlertMustBeInFactionToUseCommand",
                    false
                );
                return;
            }
        } else {
            target = this.factionRepository.get(String.join(" ", args));
            if (target == null) {
                this.playerService.sendMessage(
                    sender,
                    "&c" + this.localeService.getText("FactionNotFound"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                    true
                );
                return;
            }
        }
        this.messenger.sendFactionInfo(sender, target, target.getClaimedChunks().size());
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