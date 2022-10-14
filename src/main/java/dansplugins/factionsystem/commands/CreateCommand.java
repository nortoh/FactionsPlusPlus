/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionCreateEvent;
import dansplugins.factionsystem.factories.FactionFactory;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class CreateCommand extends SubCommand {
    private final PlayerService playerService;
    private final ConfigService configService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final Logger logger;
    private final LocaleService localeService;
    private final MedievalFactions medievalFactions;
    private final DynmapIntegrator dynmapIntegrator;
    private final FactionFactory factionFactory;

    @Inject
    public CreateCommand(
        PlayerService playerService,
        ConfigService configService,
        MessageService messageService,
        PersistentData persistentData,
        Logger logger,
        LocaleService localeService,
        MedievalFactions medievalFactions,
        DynmapIntegrator dynmapIntegrator,
        FactionFactory factionFactory
    ) {
        super();
        this.playerService = playerService;
        this.configService = configService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.logger = logger;
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.dynmapIntegrator = dynmapIntegrator;
        this.factionFactory = factionFactory;
        this
            .setNames("create", LOCALE_PREFIX + "CmdCreate")
            .requiresPermissions("mf.create")
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
        Faction playerFaction = this.playerService.getPlayerFaction(player);
        if (playerFaction != null) {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("AlreadyInFaction"),
                    "AlreadyInFaction", false);
            return;
        }

        if (args.length == 0) {
            this.playerService.sendMessage(player, "&c" + this.localeService.getText("UsageCreate"),
                    "UsageCreate", false);
            return;
        }

        final String factionName = String.join(" ", args).trim();

        final FileConfiguration config = this.configService.getConfig();

        if (factionName.length() > config.getInt("factionMaxNameLength")) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("FactionNameTooLong"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNameTooLong"))
                    .replace("#name#", factionName), true
            );
            return;
        }

        if (this.persistentData.getFaction(factionName) != null) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.localeService.getText("FactionAlreadyExists"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionAlreadyExists"))
                    .replace("#name#", factionName), true
            );
            return;
        }

        playerFaction = this.factionFactory.create(factionName, player.getUniqueId());
        playerFaction.addMember(player.getUniqueId());
        FactionCreateEvent createEvent = new FactionCreateEvent(playerFaction, player);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            this.persistentData.addFaction(playerFaction);
            this.playerService.sendMessage(
                player, 
                "&a" + getText("FactionCreated"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionCreated"))
                    .replace("#name#", factionName), true
            );
        }
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
}