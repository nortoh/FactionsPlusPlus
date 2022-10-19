/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.builders.*;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * @author Callum Johnson
 */
@Singleton
public class StatsCommand extends Command {

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
        super(
            new CommandBuilder()
                .withName("stats")
                .withAliases(LOCALE_PREFIX + "CmdStats")
                .withDescription("Retrieves plugin statistics.")
                .requiresPermissions("mf.stats")
        );
        this.configService = configService;
        this.persistentData = persistentData;
        this.playerService = playerService;
        this.messageService = messageService;
    }

    public void execute(CommandContext context) {
        CommandSender sender = context.getSender();
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