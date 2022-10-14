/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
@Singleton
public class StatsCommand extends SubCommand {

    private final ConfigService configService;
    private final PersistentData persistentData;
    private final PlayerService playerService;
    private final MessageService messageService;

    @Inject
    public StatsCommand(
        ConfigService configService,
        PersistentData persistentData,
        PlayerService playerService,
        MessageService messageService
    ) {
        super();
        this.configService = configService;
        this.persistentData = persistentData;
        this.playerService = playerService;
        this.messageService = messageService;
        this
            .setNames("stats", LOCALE_PREFIX + "CmdStats");
    }

    @Override
    public void execute(Player player, String[] args, String key) {
        this.execute((CommandSender) player, args, key);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String key) {
        if (!this.configService.getBoolean("useNewLanguageFile")) {
            sender.sendMessage(ChatColor.AQUA + "=== Medieval Factions Stats ===");
            sender.sendMessage(ChatColor.AQUA + "Number of factions: " + this.persistentData.getNumFactions());
            sender.sendMessage(ChatColor.AQUA + "Number of players: " + this.persistentData.getNumPlayers());
        } else {
            this.messageService.getLanguage().getStringList("StatsFaction")
                    .forEach(s -> {
                        if (s.contains("#faction#")) {
                            s = s.replace("#faction#", String.valueOf(this.persistentData.getNumFactions()));
                        }
                        if (s.contains("#players#")) {
                            s = s.replace("#players#", String.valueOf(this.persistentData.getNumPlayers()));
                        }
                        s = this.playerService.colorize(s);
                        this.playerService.sendMessage(sender, "", s, true);
                    });
        }
    }
}