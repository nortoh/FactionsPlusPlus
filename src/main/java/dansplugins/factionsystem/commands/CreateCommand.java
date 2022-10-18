/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionCreateEvent;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.FactionService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;


import dansplugins.factionsystem.builders.*;

/**
 * @author Callum Johnson
 */
@Singleton
public class CreateCommand extends Command {
    private final PlayerService playerService;
    private final ConfigService configService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final Logger logger;
    private final LocaleService localeService;
    private final MedievalFactions medievalFactions;
    private final FactionRepository factionRepository;
    private final FactionService factionService;

    @Inject
    public CreateCommand(
        PlayerService playerService,
        ConfigService configService,
        MessageService messageService,
        PersistentData persistentData,
        Logger logger,
        LocaleService localeService,
        MedievalFactions medievalFactions,
        FactionRepository factionRepository,
        FactionService factionService
    ) {
        super(
            new CommandBuilder()
                .withName("create")
                .withAliases(LOCALE_PREFIX + "CmdCreate")
                .withDescription("creates a new faction")
                .expectsPlayerExecution()
                .requiresPermissions("mf.create")
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("a name for your new faction")
                        .expectsString()
                        .consumesAllLaterArguments()
                        .isRequired()
                        
                )
        );
        this.playerService = playerService;
        this.configService = configService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.logger = logger;
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
    }

    public void execute(CommandContext context) {
        if (context.getExecutorsFaction() != null) {
            this.playerService.sendMessage(context.getPlayer(), "&c" + this.localeService.getText("AlreadyInFaction"),
                    "AlreadyInFaction", false);
            return;
        }
        final String factionName = (String)context.getArgument("faction name");
        final FileConfiguration config = this.configService.getConfig();
        if (factionName.length() > config.getInt("factionMaxNameLength")) {
            this.playerService.sendMessage(
                context.getPlayer(), 
                "&c" + this.localeService.getText("FactionNameTooLong"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNameTooLong"))
                    .replace("#name#", factionName), true
            );
            return;
        }

        if (this.factionRepository.get(factionName) != null) {
            this.playerService.sendMessage(
                context.getPlayer(), 
                "&c" + this.localeService.getText("FactionAlreadyExists"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionAlreadyExists"))
                    .replace("#name#", factionName), true
            );
            return;
        }

        Faction playerFaction = this.factionService.createFaction(factionName, context.getPlayer().getUniqueId());
        playerFaction.addMember(context.getPlayer().getUniqueId());
        FactionCreateEvent createEvent = new FactionCreateEvent(playerFaction, context.getPlayer());
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            this.factionRepository.create(playerFaction);
            this.playerService.sendMessage(
                context.getPlayer(), 
                "&a" + this.localeService.getText("FactionCreated"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionCreated"))
                    .replace("#name#", factionName), true
            );
        }
    }
}